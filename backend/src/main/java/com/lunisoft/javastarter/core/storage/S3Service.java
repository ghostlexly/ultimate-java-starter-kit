package com.lunisoft.javastarter.core.storage;

import com.lunisoft.javastarter.config.CacheConfig;
import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.property.S3Properties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * Uploads a file to S3. The content is given as a Spring {@link Resource} (and not as a plain
     * {@link InputStream}) because the SDK reads the payload several times — signing, checksum,
     * retries — and therefore needs to be able to reopen it. The caller must provide an accurate
     * {@code contentLength}, S3 requires it for streamed uploads.
     */
    public void upload(
            String key, Resource resource, long contentLength, String contentType, StorageClass storageClass) {
        var request = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .storageClass(storageClass)
                .build();

        var contentStreamProvider = toContentStreamProvider(resource, key);

        s3Client.putObject(request, RequestBody.fromContentProvider(contentStreamProvider, contentLength, contentType));
    }

    /**
     * Adapts a Spring {@link Resource} to the SDK provider, which cannot throw a checked exception
     * when a new stream is requested.
     */
    private ContentStreamProvider toContentStreamProvider(Resource resource, String key) {
        return () -> {
            try {
                return resource.getInputStream();
            } catch (IOException e) {
                log.error("Failed to open the file {} to upload it to storage.", key, e);

                throw new BusinessRuleException(
                        "Failed to upload the file to storage.",
                        "STORAGE_UPLOAD_ERROR",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        };
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
        } catch (Exception e) {
            log.error("Failed to download the file {} from storage.", key, e);

            throw new BusinessRuleException(
                    "Failed to download the file from storage.",
                    "STORAGE_DOWNLOAD_ERROR",
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
