package com.lunisoft.javastarter.module.demo.controller;

import com.lunisoft.javastarter.core.pdf.PdfService;
import com.lunisoft.javastarter.core.ratelimit.RateLimit;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.storage.StorageService;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.dto.BodyValidationExampleRequest;
import com.lunisoft.javastarter.module.demo.dto.DemoPaginatedCustomerResponse;
import com.lunisoft.javastarter.module.demo.dto.DemoPreviewUploadedMediasResponse;
import com.lunisoft.javastarter.module.demo.dto.DemoSearchCustomerResponse;
import com.lunisoft.javastarter.module.demo.usecase.DemoPaginateCustomerUseCase;
import com.lunisoft.javastarter.module.demo.usecase.DemoSearchCustomerUseCase;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated // Validates request parameters only (countryCode and role) (note: doesn't validate body)
@RequestMapping("/api/demo")
public class DemoController {

  private final DemoSearchCustomerUseCase demoSearchCustomerUseCase;
  private final DemoPaginateCustomerUseCase demoPaginateCustomerUseCase;
  private final MediaRepository mediaRepository;
  private final StorageService storageService;
  private final PdfService pdfService;

  /**
   * Demo endpoint: search customers by account role. Example: GET /api/demo/customers?role=CUSTOMER
   */
  @GetMapping("/customers")
  public ResponseEntity<List<DemoSearchCustomerResponse>> searchCustomers(@RequestParam Role role) {

    List<DemoSearchCustomerResponse> results = demoSearchCustomerUseCase.execute(role);

    return ResponseEntity.ok(results);
  }

  /** Verify if the */
  @PostMapping("/body-validation")
  @RateLimit(requests = 1, periodSeconds = 60)
  public ResponseEntity<Map<String, String>> bodyValidationExample(
      @Valid @RequestBody BodyValidationExampleRequest request) {
    return ResponseEntity.ok(Map.of("message", "Success", "validation", request.toString()));
  }

  /**
   * Demo endpoint: paginated list of customers with optional filters. Examples: GET
   * /api/demo/customers/paginated GET /api/demo/customers/paginated?page=1&size=10 GET
   * /api/demo/customers/paginated?email=john GET
   * /api/demo/customers/paginated?email=john&page=1&size=5
   */
  @GetMapping("/customers/paginated")
  public ResponseEntity<DemoPaginatedCustomerResponse> paginateCustomers(
      @Min(1) @RequestParam(defaultValue = "1") int page,
      @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String email) {

    DemoPaginatedCustomerResponse response =
        demoPaginateCustomerUseCase.execute(page - 1, size, email);

    return ResponseEntity.ok(response);
  }

  @GetMapping("simple-json-response")
  public ResponseEntity<Map<String, String>> simpleJsonResponse() {
    return ResponseEntity.ok(Map.of("message", "Success"));
  }

  @GetMapping("simple-message-response")
  public ResponseEntity<String> simpleMessageResponse() {
    return ResponseEntity.ok("Success");
  }

  @GetMapping("rate-limited")
  @RateLimit(requests = 5, periodSeconds = 60)
  public ResponseEntity<Map<String, String>> rateLimited() {
    return ResponseEntity.ok(
        Map.of(
            "message", "Send multiple requests to this endpoint to see rate limiting in action."));
  }

  @GetMapping("accessible-to-public")
  @PublicEndpoint
  public ResponseEntity<Map<String, String>> accessibleToPublic() {
    return ResponseEntity.ok(
        Map.of("message", "This endpoint is accessible to public without any authentication."));
  }

  @GetMapping("preview-uploaded-medias")
  public ResponseEntity<List<DemoPreviewUploadedMediasResponse>> previewUploadedMedias() {
    List<DemoPreviewUploadedMediasResponse> previewUrls =
        mediaRepository.findAll().stream()
            .map(
                media ->
                    new DemoPreviewUploadedMediasResponse(
                        media.getId(),
                        media.getFileName(),
                        media.getKey(),
                        media.getMimeType(),
                        storageService.generatePresignedGetUrl(
                            media.getKey(), Duration.ofHours(1))))
            .toList();

    return ResponseEntity.ok(previewUrls);
  }

  /**
   * Demo endpoint: generates a sample PDF using Playwright + Tailwind CSS. Display the PDF in the
   * browser.
   */
  @GetMapping("preview-pdf")
  @PublicEndpoint
  public ResponseEntity<byte[]> generatePdf() {
    var avatarUri = pdfService.toDataUri("templates/assets/avatar.png", "image/png");
    byte[] pdf =
        pdfService.generate(
            "sample-pdf",
            Map.of(
                "title", "Sample PDF Document",
                "testText", "This PDF was generated using Playwright with Tailwind CSS styling.",
                "avatarUri", avatarUri));

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sample.pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  /**
   * Demo endpoint: generates a sample PDF using Playwright + Tailwind CSS. Returns the PDF as a
   * downloadable file.
   */
  @GetMapping("download-pdf")
  @PublicEndpoint
  public ResponseEntity<byte[]> downloadPdf() {
    var avatarUri = pdfService.toDataUri("templates/assets/avatar.png", "image/png");
    byte[] pdf =
        pdfService.generate(
            "sample-pdf",
            Map.of(
                "title", "Sample PDF Document",
                "testText", "This PDF was generated using Playwright with Tailwind CSS styling.",
                "avatarUri", avatarUri));

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample.pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }
}
