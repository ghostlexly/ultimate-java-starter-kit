package com.lunisoft.javastarter.module.media.service;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.entity.Media;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {
    private final MediaRepository mediaRepository;
    private final S3Service s3Service;

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Transactional(readOnly = true)
    public String getPresignedUrl(UUID mediaId) {
        var media = mediaRepository
                .findById(mediaId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Media to get presigned URL cannot be found.",
                        "PRESIGNED_MEDIA_NOT_FOUND",
                        HttpStatus.BAD_REQUEST));

        return s3Service.generatePresignedGetUrl(media.getKey());
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
        String safeName = (fileName == null || fileName.isBlank()) ? "file" : fileName;

        int dotIndex = safeName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? safeName.substring(dotIndex).toLowerCase() : "";
        String baseName = dotIndex >= 0 ? safeName.substring(0, dotIndex) : safeName;

        String normalizedFileName = "%s-%s%s".formatted(baseName, UUID.randomUUID(), extension);
        normalizedFileName = normalizedFileName.replaceAll("[^a-zA-Z0-9.]+", "_");

        var now = ZonedDateTime.now(ZoneId.of("Europe/Paris"));

        return "%s/%s/%s".formatted(storagePath, now.format(DATE_FOLDER_FORMAT), normalizedFileName);
    }
}
