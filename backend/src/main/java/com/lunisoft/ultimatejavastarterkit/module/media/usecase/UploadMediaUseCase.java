package com.lunisoft.ultimatejavastarterkit.module.media.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.storage.StorageService;
import com.lunisoft.ultimatejavastarterkit.module.media.dto.UploadMediaResponse;
import com.lunisoft.ultimatejavastarterkit.module.media.entity.Media;
import com.lunisoft.ultimatejavastarterkit.module.media.repository.MediaRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.StorageClass;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

  private final Logger log = LoggerFactory.getLogger(UploadMediaUseCase.class);

  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
  private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofHours(1);
  private static final StorageClass STORAGE_CLASS = StorageClass.STANDARD;
  private static final String STORAGE_PATH = "media";

  private final StorageService storageService;
  private final MediaRepository mediaRepository;

  /** Validates the uploaded image, stores it in S3, and persists metadata to the database. */
  @Transactional
  public UploadMediaResponse execute(MultipartFile file) {
    validateFile(file);

    var now = Instant.now().atOffset(ZoneOffset.UTC);
    var originalFileName = file.getOriginalFilename();
    var mimeType = file.getContentType();
    var extension = extractExtension(originalFileName);
    var key =
        "%s/%s/%s.%s"
            .formatted(
                STORAGE_PATH,
                now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                UUID.randomUUID(),
                extension);

    try {
      storageService.upload(key, file.getBytes(), mimeType, STORAGE_CLASS);
    } catch (IOException e) {
      log.error("Failed to upload file to S3: {}", e.getMessage(), e);
      throw new BusinessRuleException(
          "Failed to upload file", "UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    var media = new Media();
    media.setFileName(originalFileName);
    media.setKey(key);
    media.setMimeType(mimeType);
    media.setSize(file.getSize());
    mediaRepository.save(media);

    var url = storageService.generatePresignedGetUrl(key, PRESIGNED_URL_EXPIRY);

    return new UploadMediaResponse(media.getId(), url);
  }

  private void validateFile(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessRuleException("File is empty", "FILE_EMPTY", HttpStatus.BAD_REQUEST);
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessRuleException(
          "File size exceeds the maximum allowed size of %s MB"
              .formatted(MAX_FILE_SIZE / (1024 * 1024)),
          "FILE_TOO_LARGE",
          HttpStatus.BAD_REQUEST);
    }

    var contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
      throw new BusinessRuleException(
          "File type is not allowed. Allowed types: JPEG, PNG, WebP, GIF",
          "INVALID_FILE_TYPE",
          HttpStatus.BAD_REQUEST);
    }
  }

  private String extractExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "bin";
    }

    return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
  }
}
