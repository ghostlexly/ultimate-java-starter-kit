package com.lunisoft.javastarter.module.media.usecase;

import com.lunisoft.javastarter.core.storage.StorageService;
import com.lunisoft.javastarter.module.media.entity.Media;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.StorageClass;

@Service
@RequiredArgsConstructor
public class UploadMediaUseCase {

  private static final StorageClass STORAGE_CLASS = StorageClass.STANDARD;
  private static final String STORAGE_PATH = "media";
  private static final DateTimeFormatter DATE_FOLDER_FORMAT =
      DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final StorageService storageService;
  private final MediaRepository mediaRepository;

  /**
   * Stores the provided file in S3 and persists its metadata. Generic on purpose: callers from any
   * module can supply a stream from any source (multipart upload, in-memory bytes, another S3
   * object, ...). Validation (mime type, size, ...) is the caller's responsibility.
   */
  @Transactional
  public Media execute(UploadMediaInput input) {
    var key = buildKey(input.fileName());

    var media = new Media();
    media.setFileName(input.fileName());
    media.setKey(key);
    media.setMimeType(input.contentType());
    media.setSize(input.size());
    mediaRepository.save(media);

    storageService.upload(
        key, input.inputStream(), input.size(), input.contentType(), STORAGE_CLASS);

    return media;
  }

  private String buildKey(String fileName) {
    var now = Instant.now().atOffset(ZoneOffset.UTC);

    return "%s/%s/%s.%s"
        .formatted(
            STORAGE_PATH,
            now.format(DATE_FOLDER_FORMAT),
            UUID.randomUUID(),
            extractExtension(fileName));
  }

  private String extractExtension(String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "bin";
    }

    return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
  }
}
