package com.lunisoft.javastarter.module.demo.controller;

import com.lunisoft.javastarter.config.CacheConfig;
import com.lunisoft.javastarter.core.dto.MessageResponse;
import com.lunisoft.javastarter.core.pdf.PdfService;
import com.lunisoft.javastarter.core.ratelimit.RateLimit;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.storage.StorageService;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.dto.BodyValidationExampleRequest;
import com.lunisoft.javastarter.module.demo.dto.DemoPreviewUploadedMediasResponse;
import com.lunisoft.javastarter.module.demo.usecase.DemoJobRunrEnqueueJob;
import com.lunisoft.javastarter.module.demo.usecase.GetCachedTimeUseCase;
import com.lunisoft.javastarter.module.demo.usecase.PaginateCustomersUseCase;
import com.lunisoft.javastarter.module.demo.usecase.SearchCustomersUseCase;
import com.lunisoft.javastarter.module.media.repository.MediaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import lombok.RequiredArgsConstructor;
import org.jobrunr.scheduling.BackgroundJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated // Validates request parameters only (countryCode and role) (note: doesn't validate body)
@RequestMapping("/api/demo")
public class DemoController {

  private static final Logger log = LoggerFactory.getLogger(DemoController.class);
  private final SearchCustomersUseCase searchCustomersUseCase;
  private final PaginateCustomersUseCase paginateCustomersUseCase;
  private final MediaRepository mediaRepository;
  private final StorageService storageService;
  private final PdfService pdfService;
  private final RedisLockRegistry lockRegistry;
  private final DemoJobRunrEnqueueJob demoJobRunrEnqueueJob;
  private final GetCachedTimeUseCase getCachedTimeUseCase;

  /**
   * Demo endpoint: search customers by account role. Example: GET
   * /api/demo/customers?role=CUSTOMER
   */
  @GetMapping("customers")
  public ResponseEntity<List<SearchCustomersUseCase.Output>> searchCustomers(
      @RequestParam Role role) {

    var input = new SearchCustomersUseCase.Input(Role.CUSTOMER);
    List<SearchCustomersUseCase.Output> outputs = this.searchCustomersUseCase.execute(input);

    return ResponseEntity.ok(outputs);
  }

  /**
   * Validate the body of the request. The rate limiter run only when the request is valid.
   */
  @PostMapping("body-validation")
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
  @GetMapping("customers/paginated")
  public ResponseEntity<PaginateCustomersUseCase.Output> paginateCustomers(
      @Min(1) @RequestParam(defaultValue = "1") int page,
      @Min(1) @Max(100) @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String email) {

    var input = new PaginateCustomersUseCase.Input(page - 1, size, email);

    var response = this.paginateCustomersUseCase.execute(input);

    return ResponseEntity.ok(response);
  }

  @GetMapping("simple-json-response")
  public ResponseEntity<MessageResponse> simpleJsonResponse() {
    return ResponseEntity.ok(new MessageResponse("Success"));
  }

  @GetMapping("simple-message-response")
  public ResponseEntity<String> simpleMessageResponse() {
    return ResponseEntity.ok("Success");
  }

  @GetMapping("lock")
  public ResponseEntity<MessageResponse> lockTest() throws InterruptedException {
    Lock lock = this.lockRegistry.obtain("test-lock");
    lock.lock();

    log.info("Lock acquired !");

    try {
      // wait for 5 seconds to simulate doing something
      Thread.sleep(5_000);
    } finally {
      lock.unlock();
    }

    return ResponseEntity.ok(new MessageResponse("Lock acquired"));
  }

  @GetMapping("rate-limited")
  @RateLimit(requests = 5, periodSeconds = 60)
  public ResponseEntity<MessageResponse> rateLimited() {
    return ResponseEntity.ok(
        new MessageResponse(
            "Send multiple requests to this endpoint to see rate limiting in action."));
  }

  @GetMapping("accessible-to-public")
  @PublicEndpoint
  public ResponseEntity<MessageResponse> accessibleToPublic() {
    return ResponseEntity.ok(
        new MessageResponse("This endpoint is accessible to public without any authentication."));
  }

  @GetMapping("jobrunr-demo")
  @PublicEndpoint
  public ResponseEntity<MessageResponse> jobrunrDemo() {
    BackgroundJob.enqueue(() -> this.demoJobRunrEnqueueJob.execute("abcdef"));

    return ResponseEntity.ok(new MessageResponse("The new job has been scheduled."));
  }

  @GetMapping("preview-uploaded-medias")
  public ResponseEntity<List<DemoPreviewUploadedMediasResponse>> previewUploadedMedias() {
    List<DemoPreviewUploadedMediasResponse> previewUrls =
        this.mediaRepository.findAll().stream()
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
    var avatarUri = this.pdfService.toDataUri("templates/assets/avatar.png", "image/png");
    byte[] pdf =
        this.pdfService.generate(
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
    var avatarUri = this.pdfService.toDataUri("templates/assets/avatar.png", "image/png");
    byte[] pdf =
        this.pdfService.generate(
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

  @GetMapping("cached")
  @PublicEndpoint
  public ResponseEntity<GetCachedTimeUseCase.Output> cached() {
    return ResponseEntity.ok(this.getCachedTimeUseCase.execute());
  }

  @GetMapping("evict-cache")
  @PublicEndpoint
  @CacheEvict(value = CacheConfig.CACHED_TIME)
  public ResponseEntity<Map<String, String>> evictCache() {
    return ResponseEntity.ok(Map.of("message", "Cache cleared"));
  }
}
