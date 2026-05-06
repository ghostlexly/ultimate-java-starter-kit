package com.lunisoft.javastarter.module.media.service;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import java.io.InputStream;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MediaSecurityService {
  private final MediaRepository mediaRepository;
  private final Tika tika = new Tika();

  public String detectContentType(InputStream inputStream) {
    try {
      return tika.detect(inputStream);
    } catch (Exception ex) {
      throw new BusinessRuleException(
          "Failed to detect content type: %s".formatted(ex.getMessage()),
          "MIME_TYPE_DETECTION_FAILED",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void validateImageMedia(String contentType, long fileSize) {
    Set<String> allowedContentTypes = Set.of("image/jpeg", "image/png", "application/pdf");
    long maxFileSize = 10L * 1024 * 1024; // 10 MB

    if (!allowedContentTypes.contains(contentType)) {
      throw new BusinessRuleException(
          "File type is not allowed. Allowed types: JPEG, PNG, WebP, PDF",
          "INVALID_FILE_TYPE",
          HttpStatus.BAD_REQUEST);
    }

    if (fileSize > maxFileSize) {
      throw new BusinessRuleException(
          "File size exceeds the maximum allowed size of %s MB"
              .formatted(maxFileSize / (1024 * 1024)),
          "FILE_TOO_LARGE",
          HttpStatus.BAD_REQUEST);
    }
  }
}
