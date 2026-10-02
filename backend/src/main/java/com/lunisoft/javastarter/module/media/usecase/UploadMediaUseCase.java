package com.lunisoft.javastarter.module.media.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import com.lunisoft.javastarter.module.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

    private static final StorageClass STORAGE_CLASS = StorageClass.STANDARD;
    private static final String STORAGE_PATH = "media";

    private final S3Service s3Service;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    public record UploadMediaCommand(Resource resource, String fileName, String contentType, long size) {}

    public record UploadMediaResult(UUID id, String key) {}

    /**
     * Stores the provided file in S3 and persists its metadata. Generic on purpose: callers from any
     * module can supply a resource from any source (multipart upload, in-memory bytes, a file, ...).
     * Validation (mime type, size, ...) is the caller's responsibility.
     */
    public UploadMediaResult execute(UploadMediaCommand command) {
        var key = mediaService.buildKey(STORAGE_PATH, command.fileName());

        uploadToStorage(key, command);

        var media = mediaRepository.save(new Media(command.fileName(), key, command.contentType(), command.size()));

        return new UploadMediaResult(media.getId(), media.getKey());
    }

    /**
     * Streams the resource to S3. The use case opens its own stream and closes it once the upload
     * is complete, so callers never have to manage it.
     */
    private void uploadToStorage(String key, UploadMediaCommand command) {
        try (InputStream inputStream = command.resource().getInputStream()) {
            s3Service.upload(key, inputStream, command.contentType(), STORAGE_CLASS);
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    "Failed to read uploaded file: %s".formatted(ex.getMessage()),
                    "UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
