package com.lunisoft.javastarter.module.media.controller;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.storage.StorageService;
import com.lunisoft.javastarter.module.media.dto.UploadMediaResponse;
import com.lunisoft.javastarter.module.media.service.MediaSecurityService;
import com.lunisoft.javastarter.module.media.usecase.UploadMediaInput;
import com.lunisoft.javastarter.module.media.usecase.UploadMediaUseCase;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@PublicEndpoint
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
public class MediaController {

  private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofHours(1);

  private final UploadMediaUseCase uploadMediaUseCase;
  private final MediaSecurityService mediaSecurityService;
  private final StorageService storageService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadMediaResponse> upload(@RequestParam("file") MultipartFile file) {
    try {
      String contentType = mediaSecurityService.detectContentType(file.getInputStream());

      mediaSecurityService.validateImageMedia(contentType, file.getSize());

      var input =
          new UploadMediaInput(
              file.getInputStream(), file.getOriginalFilename(), contentType, file.getSize());

      var media = uploadMediaUseCase.execute(input);

      var url = storageService.generatePresignedGetUrl(media.getKey(), PRESIGNED_URL_EXPIRY);

      return ResponseEntity.ok(new UploadMediaResponse(media.getId(), url));
    } catch (IOException ex) {
      throw new BusinessRuleException(
          "Failed to read uploaded file: %s".formatted(ex.getMessage()),
          "UPLOAD_FAILED",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
