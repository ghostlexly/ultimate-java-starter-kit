package com.lunisoft.javastarter.module.media.service;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaSecurityService {
  private final MediaRepository mediaRepository;
  private final Tika tika = new Tika();

  public void validateImageMedia(MultipartFile file) {
    Set<String> allowedMimeTypes = Set.of("image/jpeg", "image/png", "application/pdf");
    long maxFileSize = 10L * 1024 * 1024; // 10 MB

    String detectedMimeType;

    try (InputStream inputStream = file.getInputStream()) {
      detectedMimeType = tika.detect(inputStream);
    } catch (IOException e) {
      throw new BusinessRuleException(
          "Failed to detect file type",
          "FILE_TYPE_DETECTION_ERROR",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }

    if (!allowedMimeTypes.contains(detectedMimeType)) {
      throw new BusinessRuleException(
          "File type is not allowed. Allowed types: JPEG, PNG, WebP, PDF",
          "INVALID_FILE_TYPE",
          HttpStatus.BAD_REQUEST);
    }

    if (file.getSize() > maxFileSize) {
      throw new BusinessRuleException(
          "File size exceeds the maximum allowed size of %s MB"
              .formatted(maxFileSize / (1024 * 1024)),
          "FILE_TOO_LARGE",
          HttpStatus.BAD_REQUEST);
    }
  }
}
