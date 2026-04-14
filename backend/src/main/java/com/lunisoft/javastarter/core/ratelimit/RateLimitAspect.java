package com.lunisoft.javastarter.core.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunisoft.javastarter.core.dto.ErrorResponse;
import com.lunisoft.javastarter.core.security.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Enforces @RateLimit via AOP. Because Spring resolves and validates controller arguments
 * (including @Valid @RequestBody) BEFORE invoking the method, this advice only runs for requests
 * that pass validation — invalid payloads do not consume a token.
 */
@Aspect
@Component
public class RateLimitAspect {
  private final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

  /** Wraps a bucket with its last access time for eviction. */
  private record BucketEntry(Bucket bucket, Instant lastAccess) {}

  private static final Duration EVICTION_THRESHOLD = Duration.ofMinutes(10);

  private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Around("@annotation(rateLimit)")
  public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
    HttpServletRequest request = currentRequest();
    HttpServletResponse response = currentResponse();
    String key = buildKey(pjp, request);

    BucketEntry entry =
        buckets.compute(
            key,
            (_, existing) -> {
              Bucket bucket = (existing != null) ? existing.bucket() : createBucket(rateLimit);

              return new BucketEntry(bucket, Instant.now());
            });

    ConsumptionProbe probe = entry.bucket().tryConsumeAndReturnRemaining(1);

    if (!probe.isConsumed()) {
      long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
      writeRateLimitResponse(response, waitSeconds);

      // Short-circuit the controller — Spring will not write another body
      // because the response is already committed.
      return null;
    }

    response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));

    // Proceed with the controller method; if it throws, the token stays consumed
    // (business failures still count against the limit — only arg-resolution
    // failures like @Valid never reach this advice).
    return pjp.proceed();
  }

  /** Writes a 429 Too Many Requests response with Retry-After header. */
  private void writeRateLimitResponse(HttpServletResponse response, long waitSeconds)
      throws java.io.IOException {
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

  /** Builds a unique key per endpoint per client. */
  private String buildKey(ProceedingJoinPoint pjp, HttpServletRequest request) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();

    String endpoint = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
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

  private HttpServletRequest currentRequest() {

    return currentAttrs().getRequest();
  }

  private HttpServletResponse currentResponse() {
    HttpServletResponse response = currentAttrs().getResponse();

    if (response == null) {
      throw new IllegalStateException(
          "No current HTTP response — @RateLimit requires a servlet context");
    }

    return response;
  }

  private ServletRequestAttributes currentAttrs() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attrs == null) {
      throw new IllegalStateException(
          "No current HTTP request — @RateLimit requires a servlet context");
    }

    return attrs;
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
