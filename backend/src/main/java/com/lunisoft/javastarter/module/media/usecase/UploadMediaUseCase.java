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
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

    private static final StorageClass STORAGE_CLASS = StorageClass.STANDARD;
    private static final String STORAGE_PATH = "media";

    private final S3Service s3Service;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    public record Input(Resource resource, String fileName, String contentType, long size) {}

    /**
     * Stores the provided file in S3 and persists its metadata. Generic on purpose: callers from any
     * module can supply a resource from any source (multipart upload, in-memory bytes, a file, ...).
     * Validation (mime type, size, ...) is the caller's responsibility.
     */
    @Transactional
    public Media execute(Input input) {
        var key = mediaService.buildKey(STORAGE_PATH, input.fileName());

        var media = new Media(input.fileName(), key, input.contentType(), input.size());
        mediaRepository.save(media);

        uploadToStorage(key, input);

        return media;
    }

    /**
     * Streams the resource to S3. The use case opens its own stream and closes it once the upload
     * is complete, so callers never have to manage it.
     */
    private void uploadToStorage(String key, Input input) {
        try (InputStream inputStream = input.resource().getInputStream()) {
            s3Service.upload(key, inputStream, input.contentType(), STORAGE_CLASS);
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    "Failed to read uploaded file: %s".formatted(ex.getMessage()),
                    "UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
