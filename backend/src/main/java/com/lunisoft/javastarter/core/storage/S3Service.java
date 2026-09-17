package com.lunisoft.javastarter.core.storage;

import com.lunisoft.javastarter.config.CacheConfig;
import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.property.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * Uploads a file of any size to S3 from an {@link InputStream}, without knowing its length.
     *
     * <p>The upload goes through the SDK's multipart async client, which streams the content in
     * parts (8 MB by default, i.e. up to ~80 GB with the S3 limit of 10 000 parts) without loading
     * the whole file in memory. The call blocks until the upload is complete. The caller remains
     * responsible for closing the stream.
     */
    public void upload(String key, InputStream inputStream, String contentType, StorageClass storageClass) {
        var request = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .storageClass(storageClass)
                .build();

        // A null content length means "unknown": the SDK splits the stream into parts on the fly
        var body = AsyncRequestBody.forBlockingInputStream(null);
        var upload = s3AsyncClient.putObject(request, body);

        body.writeInputStream(inputStream);
        upload.join();
    }

    /**
     * Downloads a file from S3 as a stream. The caller is responsible for closing the stream (prefer
     * try-with-resources). Streaming avoids loading the whole object in memory, which is important
     * for large files.
     */
    public InputStream download(String key) {
        var request = GetObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        return s3Client.getObject(request);
    }

    /**
     * Downloads a file from S3 fully in memory. Only for small files — prefer {@link #download} and
     * stream the content when the size is not bounded.
     */
    public byte[] downloadAsBytes(String key) {
        try (InputStream inputStream = download(key)) {
            return inputStream.readAllBytes();
        } catch (Exception _) {
            throw new BusinessRuleException(
                    "Failed to download the file from storage.",
                    "STORAGE_DOWNLOAD_AS_BYTES_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Downloads a file from S3 and returns it Base64-encoded (e.g. for MangoPay KYC pages).
     */
    public String downloadAsBase64(String key) {
        return Base64.getEncoder().encodeToString(downloadAsBytes(key));
    }

    /**
     * Deletes a file from S3.
     */
    public void delete(String key) {
        var request = DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * Generates a presigned URL to preview/download a file.
     */
    @Cacheable(value = CacheConfig.S3_PRESIGNED_GET_URL, key = "#key")
    public String generatePresignedGetUrl(String key) {
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(25))
                .getObjectRequest(r -> r.bucket(s3Properties.bucket()).key(key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Generates a presigned URL to upload a file directly to S3.
     */
    public String generatePresignedPutUrl(String key, String contentType, Duration expiry) {
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(r -> r.bucket(s3Properties.bucket()).key(key).contentType(contentType))
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }
}
