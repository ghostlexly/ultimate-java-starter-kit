package com.lunisoft.ultimatejavastarterkit.core.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunisoft.ultimatejavastarterkit.core.dto.ErrorResponse;
import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts requests to @RateLimit-annotated controller methods. Creates a token bucket per client
 * (IP or user ID) per endpoint.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
  private final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

  /** Wraps a bucket with its last access time for eviction. */
  private record BucketEntry(Bucket bucket, Instant lastAccess) {}

  private static final Duration EVICTION_THRESHOLD = Duration.ofMinutes(10);

  private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler)
      throws IOException {
    // Only intercept controller methods
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    // Check if the method has @RateLimit
    RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
    if (rateLimit == null) {
      return true;
    }

    String key = buildKey(handlerMethod, request);

    // Get or create the bucket, and update last access time
    BucketEntry entry =
        buckets.compute(
            key,
            (k, existing) -> {
              Bucket bucket = (existing != null) ? existing.bucket() : createBucket(rateLimit);

              return new BucketEntry(bucket, Instant.now());
            });

    ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);

    if (probe.isConsumed()) {
      response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

      return true;
    }

    // Rate limit exceeded — write 429 response directly
    long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
    writeRateLimitResponse(response, waitSeconds);

    return false;
  }

  /** Writes a 429 Too Many Requests response with Retry-After header. */
  private void writeRateLimitResponse(HttpServletResponse response, long waitSeconds)
      throws IOException {
    ErrorResponse errorResponse =
        new ErrorResponse(
            "RateLimitException",
            "Rate limit exceeded. Try again in %s seconds.".formatted(waitSeconds),
            "RATE_LIMIT_EXCEEDED",
            null);

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(waitSeconds));
    objectMapper.writeValue(response.getWriter(), errorResponse);
  }

  /**
   * Builds a unique key per endpoint per client. Format: "ControllerName#method:user:accountId" or
   * "ControllerName#method:ip:address"
   */
  private String buildKey(HandlerMethod handlerMethod, HttpServletRequest request) {
    String endpoint =
        handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    String clientId = resolveClientId(request);

    return endpoint + ":" + clientId;
  }

  /** Returns "user:<accountId>" if authenticated, otherwise "ip:<address>". */
  private String resolveClientId(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
      return "user:" + principal.accountId();
    }

    return "ip:" + resolveIpAddress(request);
  }

  /** Reads the client IP from X-Forwarded-For header or falls back to remoteAddr. */
  private String resolveIpAddress(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");

    if (forwarded != null && !forwarded.isBlank()) {
      // Take the first IP in the chain (original client)
      return forwarded.split(",")[0].trim();
    }

    return request.getRemoteAddr();
  }

  private Bucket createBucket(RateLimit rateLimit) {

    return Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(rateLimit.requests())
                .refillGreedy(rateLimit.requests(), Duration.ofSeconds(rateLimit.periodSeconds()))
                .build())
        .build();
  }

  /** Removes buckets that haven't been accessed in the last 10 minutes. */
  @Scheduled(fixedRate = 600_000)
  public void evictIdleBuckets() {
    Instant threshold = Instant.now().minus(EVICTION_THRESHOLD);
    int before = buckets.size();

    buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess().isBefore(threshold));

    int removed = before - buckets.size();
    if (removed > 0) {
      log.debug("Evicted {} idle rate limit buckets", removed);
    }
  }
}
