package com.lunisoft.ultimatejavastarterkit.core.storage;

import com.lunisoft.ultimatejavastarterkit.config.StorageProperties;
import java.io.IOException;
import java.time.Duration;
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
public class StorageService {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final StorageProperties storageProperties;

  public StorageService(
      S3Client s3Client, S3Presigner s3Presigner, StorageProperties storageProperties) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.storageProperties = storageProperties;
  }

  /** Uploads a file to S3. */
  public void upload(String key, byte[] data, String contentType, StorageClass storageClass) {
    var request =
        PutObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .storageClass(storageClass)
            .build();

    s3Client.putObject(request, RequestBody.fromBytes(data));
  }

  /** Downloads a file from S3 as a byte array. */
  public byte[] download(String key) throws IOException {
    var request = GetObjectRequest.builder().bucket(storageProperties.bucket()).key(key).build();

    try (var response = s3Client.getObject(request)) {

      return response.readAllBytes();
    }
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
