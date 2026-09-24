package com.lunisoft.javastarter.unit.core.storage;

import com.lunisoft.javastarter.core.storage.S3Service ;
import com.lunisoft.javastarter.property.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private S3Presigner s3Presigner;

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        var s3Properties = new S3Properties("http://localhost:9000", "us-east-1", "access", "secret", BUCKET);

        s3Service = new S3Service(s3Client, s3AsyncClient, s3Presigner, s3Properties);
    }

    @Test
    void upload_streams_whole_content_with_unknown_length() {
        var content = buildRandomContent(1024 * 1024);
        var uploadedContent = new ByteArrayOutputStream();

        givenPutObjectConsumesBody(
                uploadedContent,
                CompletableFuture.completedFuture(PutObjectResponse.builder().build()));

        s3Service.upload("media/picture.png", new ByteArrayInputStream(content), "image/png", StorageClass.STANDARD);

        var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        var bodyCaptor = ArgumentCaptor.forClass(AsyncRequestBody.class);
        verify(s3AsyncClient).putObject(requestCaptor.capture(), bodyCaptor.capture());

        // Verify that the request targets the right object with the given metadata
        assertThat(requestCaptor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(requestCaptor.getValue().key()).isEqualTo("media/picture.png");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(requestCaptor.getValue().storageClass()).isEqualTo(StorageClass.STANDARD);

        // Verify that no content length is declared, so the SDK takes the multipart streaming path
        assertThat(bodyCaptor.getValue().contentLength()).isEmpty();

        // Verify that the stream was transmitted untouched
        assertThat(uploadedContent.toByteArray()).isEqualTo(content);
    }

    @Test
    void upload_failing_request_propagates_the_sdk_error() {
        var content = buildRandomContent(1024);

        givenPutObjectConsumesBody(
                new ByteArrayOutputStream(),
                CompletableFuture.failedFuture(SdkClientException.create("Connection lost")));

        assertThatThrownBy(() -> s3Service.upload(
                        "media/picture.png", new ByteArrayInputStream(content), "image/png", StorageClass.STANDARD))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }

    /**
     * Mimics the SDK: subscribes to the request body (the upload blocks until someone consumes it),
     * collects the received bytes and answers with the given result.
     */
    private void givenPutObjectConsumesBody(
            ByteArrayOutputStream uploadedContent, CompletableFuture<PutObjectResponse> result) {
        when(s3AsyncClient.putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class)))
                .thenAnswer(invocation -> {
                    AsyncRequestBody body = invocation.getArgument(1);
                    body.subscribe(buffer -> collect(buffer, uploadedContent));

                    return result;
                });
    }

    private void collect(ByteBuffer buffer, ByteArrayOutputStream uploadedContent) {
        var bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        uploadedContent.writeBytes(bytes);
    }

    // Random bytes, so that any lost, duplicated or shifted byte is detected
    private byte[] buildRandomContent(int size) {
        var content = new byte[size];
        new Random(42).nextBytes(content);

        return content;
    }
}
