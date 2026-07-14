package com.lunisoft.javastarter.module.media.service;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final S3Service s3Service;

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Duration PRESIGNED_URL_EXPIRATION = Duration.ofHours(4);

    @Transactional(readOnly = true)
    public String getPresignedUrl(UUID mediaId, Duration presignedUrlExpiration) {
        return generatePresignedUrl(mediaId, presignedUrlExpiration);
    }

    @Transactional(readOnly = true)
    public String getPresignedUrl(UUID mediaId) {
        return generatePresignedUrl(mediaId, PRESIGNED_URL_EXPIRATION);
    }

    private String generatePresignedUrl(UUID mediaId, Duration presignedUrlExpiration) {
        var media = mediaRepository
                .findById(mediaId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Media to get presigned URL cannot be found.",
                        "PRESIGNED_MEDIA_NOT_FOUND",
                        HttpStatus.BAD_REQUEST));

        return s3Service.generatePresignedGetUrl(media.getKey(), presignedUrlExpiration);
    }

    /** Deletes a media: removes the DB row first, then the S3 object (rolls back the row on failure). */
    @Transactional
    public void delete(UUID id) {
        Media media = mediaRepository
                .findById(id)
                .orElseThrow(() -> new BusinessRuleException(
                        "Media to delete cannot be found.", "MEDIA_TO_DELETE_NOT_FOUND", HttpStatus.BAD_REQUEST));

        mediaRepository.delete(media);
        s3Service.delete(media.getKey());
    }

    public String buildKey(String storagePath, String fileName) {
        var now = Instant.now().atOffset(ZoneOffset.UTC);

        return "%s/%s/%s.%s"
                .formatted(storagePath, now.format(DATE_FOLDER_FORMAT), UUID.randomUUID(), extractExtension(fileName));
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }

        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
