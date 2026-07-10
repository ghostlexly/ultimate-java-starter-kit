package com.lunisoft.javastarter.module.media.controller;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.storage.S3Service;
import com.lunisoft.javastarter.module.media.dto.UploadMediaResponse;
import com.lunisoft.javastarter.module.media.service.MediaSecurityService;
import com.lunisoft.javastarter.module.media.usecase.UploadMediaUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@PublicEndpoint
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
public class MediaController {

    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofHours(1);

    private final UploadMediaUseCase uploadMediaUseCase;
    private final MediaSecurityService mediaSecurityService;
    private final S3Service s3Service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadMediaResponse> upload(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = this.mediaSecurityService.getContentType(file.getInputStream());

            this.mediaSecurityService.validateImageMedia(contentType, file.getSize());

            var input = new UploadMediaUseCase.Input(
                    file.getInputStream(), file.getOriginalFilename(), contentType, file.getSize());

            var output = this.uploadMediaUseCase.execute(input);

            var url = this.s3Service.generatePresignedGetUrl(output.getKey(), PRESIGNED_URL_EXPIRY);

            return ResponseEntity.ok(new UploadMediaResponse(output.getId(), url));
        } catch (IOException ex) {
            throw new BusinessRuleException(
                    "Failed to read uploaded file: %s".formatted(ex.getMessage()),
                    "UPLOAD_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
