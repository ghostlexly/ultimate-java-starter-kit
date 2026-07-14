package com.lunisoft.javastarter.module.media.usecase;

import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import com.lunisoft.javastarter.module.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

    private static final StorageClass STORAGE_CLASS = StorageClass.STANDARD;
    private static final String STORAGE_PATH = "media";

    private final S3Service s3Service;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;

    public record Input(InputStream inputStream, String fileName, String contentType, long size) {}

    /**
     * Stores the provided file in S3 and persists its metadata. Generic on purpose: callers from any
     * module can supply a stream from any source (multipart upload, in-memory bytes, another S3
     * object, ...). Validation (mime type, size, ...) is the caller's responsibility.
     */
    @Transactional
    public Media execute(Input input) {
        var key = mediaService.buildKey(STORAGE_PATH, input.fileName());

        var media = new Media();
        media.setFileName(input.fileName());
        media.setKey(key);
        media.setMimeType(input.contentType());
        media.setSize(input.size());
        mediaRepository.save(media);

        s3Service.upload(key, input.inputStream(), input.size(), input.contentType(), STORAGE_CLASS);

        return media;
    }
}
