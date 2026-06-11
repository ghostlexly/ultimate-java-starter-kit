package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.core.dto.ErrorResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared in-memory token-bucket store for rate limiting. Holds one bucket per key, evicts idle
 * buckets to bound memory usage, and centralizes client IP resolution and the 429 response format.
 *
 * <p>Used by both {@link GlobalRateLimitFilter} (global per-IP limit on every request) and {@link
 * RateLimitAspect} (stricter per-endpoint limits via {@link RateLimit}).
 */
@Component
public class RateLimitService {

  private final Logger log = LoggerFactory.getLogger(RateLimitService.class);

  /**
   * Wraps a bucket with its last access time for eviction.
   */
  private record BucketEntry(Bucket bucket, Instant lastAccess) {

  }

  private static final Duration EVICTION_THRESHOLD = Duration.ofMinutes(10);

  private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Tries to consume one token from the bucket identified by {@code key}, creating the bucket on
   * first access with the given capacity and refill period.
   */
  public ConsumptionProbe tryConsume(String key, long requests, Duration period) {
    BucketEntry entry =
        buckets.compute(
            key,
            (_, existing) -> {
              Bucket bucket =
                  (existing != null) ? existing.bucket() : createBucket(requests, period);

              return new BucketEntry(bucket, Instant.now());
            });

    return entry.bucket().tryConsumeAndReturnRemaining(1);
  }

  /**
   * Writes a 429 Too Many Requests response with Retry-After header.
   */
  public void writeRateLimitResponse(HttpServletResponse response, ConsumptionProbe probe)
      throws IOException {
    long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
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
   * Reads the client IP from header or falls back to remoteAddr.
   */
  public String resolveIpAddress(HttpServletRequest request) {
    // Secure Customer's IP Address sent by Cloudflare
    String cfIp = request.getHeader("CF-Connecting-IP");

    if (cfIp != null && !cfIp.isBlank()) {
      return cfIp.trim();
    }

    // IP Address sent by a proxy
    String forwarded = request.getHeader("X-Forwarded-For");

    if (forwarded != null && !forwarded.isBlank()) {
      // Take the first IP in the chain (original client)
      return forwarded.split(",")[0].trim();
    }

    // IP Address sent by the client
    return request.getRemoteAddr();
  }

  private Bucket createBucket(long requests, Duration period) {

    return Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(requests).refillGreedy(requests, period).build())
        .build();
  }

  /**
   * Removes buckets that haven't been accessed in the last 10 minutes.
   */
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
