package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.core.dto.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared in-memory token-bucket store for rate limiting. Holds one bucket per key, evicts idle
 * buckets to bound memory usage, and centralizes client IP resolution and the 429 response format.
 *
 * <p>Used by both {@link GlobalRateLimitFilter} (global per-IP limit on every request) and {@link
 * RateLimitAspect} (stricter per-endpoint limits via {@link RateLimit}).
 */
@Component
public class RateLimitService {

    /**
     * Wraps a bucket with its last access time for eviction.
     */
    private record BucketEntry(Bucket bucket, Instant lastAccess) {}

    private static final String CLOUDFLARE_IP_HEADER = "CF-Connecting-IP";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final Duration EVICTION_THRESHOLD = Duration.ofMinutes(10);

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Tries to consume one token from the bucket identified by {@code key}, creating the bucket on
     * first access with the given capacity and refill period.
     */
    public ConsumptionProbe tryConsume(String key, long requests, Duration period) {
        BucketEntry entry = buckets.compute(key, (_, existing) -> {
            Bucket bucket = (existing != null) ? existing.bucket() : createBucket(requests, period);

            return new BucketEntry(bucket, Instant.now());
        });

        return entry.bucket().tryConsumeAndReturnRemaining(1);
    }

    /**
     * Writes a 429 Too Many Requests response with Retry-After header.
     */
    public void writeRateLimitResponse(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000 + 1;
        ErrorResponse errorResponse = new ErrorResponse(
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
        String cfIp = request.getHeader(CLOUDFLARE_IP_HEADER);

        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        // IP Address sent by a proxy
        String forwarded = request.getHeader(FORWARDED_FOR_HEADER);

        if (forwarded != null && !forwarded.isBlank()) {
            // Take the first IP in the chain (original client)
            return forwarded.split(",")[0].trim();
        }

        // IP Address sent by the client
        return request.getRemoteAddr();
    }

    /**
     * Returns true for trusted server-to-server traffic (e.g. Next.js SSR — all SSR requests share
     * the frontend server's IP and would otherwise exhaust a single bucket): the request was not
     * forwarded by a proxy.
     *
     * <p>Browser traffic also reaches us from a private address (the Caddy container), but always
     * carries the forwarding headers proxies append — a client cannot remove them — so it is never
     * treated as internal.
     */
    public boolean isTrustedInternalRequest(HttpServletRequest request) {
        boolean isForwardedByProxy =
                request.getHeader(FORWARDED_FOR_HEADER) != null || request.getHeader(CLOUDFLARE_IP_HEADER) != null;

        return !isForwardedByProxy;
    }

    private Bucket createBucket(long requests, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requests)
                        .refillGreedy(requests, period)
                        .build())
                .build();
    }

    /**
     * Removes buckets that haven't been accessed in the last 10 minutes.
     */
    @Scheduled(fixedRate = 600_000)
    public void evictIdleBuckets() {
        Instant threshold = Instant.now().minus(EVICTION_THRESHOLD);
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess().isBefore(threshold));
    }
}
