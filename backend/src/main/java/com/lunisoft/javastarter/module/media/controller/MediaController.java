package com.lunisoft.javastarter.module.media.controller;

import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.module.media.dto.UploadMediaResponse;
import com.lunisoft.javastarter.module.media.usecase.UploadMediaUseCase;
import lombok.RequiredArgsConstructor;
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

  private final UploadMediaUseCase uploadMediaUseCase;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UploadMediaResponse> upload(@RequestParam("file") MultipartFile file) {
    var response = uploadMediaUseCase.execute(file);

    return ResponseEntity.ok(response);
  }
}
