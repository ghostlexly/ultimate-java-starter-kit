package com.lunisoft.javastarter.core.storage;

import com.lunisoft.javastarter.property.StorageProperties;
import java.io.InputStream;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class StorageService {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final StorageProperties storageProperties;

  /**
   * Uploads a file to S3 from an {@link InputStream}. The caller must provide an accurate {@code
   * contentLength} (S3 requires it for streamed uploads). The stream is consumed by the SDK; the
   * caller remains responsible for closing it.
   */
  public void upload(
      String key,
      InputStream inputStream,
      long contentLength,
      String contentType,
      StorageClass storageClass) {
    var request =
        PutObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .storageClass(storageClass)
            .build();

    s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
  }

  /**
   * Downloads a file from S3 as a stream. The caller is responsible for closing the stream (prefer
   * try-with-resources). Streaming avoids loading the whole object in memory, which is important
   * for large files.
   */
  public InputStream download(String key) {
    var request = GetObjectRequest.builder().bucket(storageProperties.bucket()).key(key).build();

    return s3Client.getObject(request);
  }

  /** Deletes a file from S3. */
  public void delete(String key) {
    var request = DeleteObjectRequest.builder().bucket(storageProperties.bucket()).key(key).build();

    s3Client.deleteObject(request);
  }

  /** Generates a presigned URL to preview/download a file. */
  public String generatePresignedGetUrl(String key, Duration expiry) {
    var presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .getObjectRequest(r -> r.bucket(storageProperties.bucket()).key(key))
            .build();

    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }

  /** Generates a presigned URL to upload a file directly to S3. */
  public String generatePresignedPutUrl(String key, String contentType, Duration expiry) {
    var presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(expiry)
            .putObjectRequest(
                r -> r.bucket(storageProperties.bucket()).key(key).contentType(contentType))
            .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }
}
