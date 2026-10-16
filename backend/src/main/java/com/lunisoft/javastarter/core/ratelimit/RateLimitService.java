package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.core.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Redis-backed fixed-window rate limiter, same model as express-rate-limit: one counter per key,
 * incremented on every hit and expired at the end of the window. Counters live in Redis so every
 * application instance shares the same quota.
 *
 * <p>Also centralizes client IP resolution and the 429 response format. Used by both {@link
 * GlobalRateLimitFilter} (global per-IP limit on every request) and {@link RateLimitAspect}
 * (stricter per-endpoint limits via {@link RateLimit}).
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String CLOUDFLARE_IP_HEADER = "CF-Connecting-IP";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String KEY_PREFIX = "rate-limit:";

    /**
     * Atomically increments the counter, starts the window on the first hit, and returns
     * {count, ttl}. The TTL guard re-arms the expiry if it was ever lost (e.g. the key was
     * persisted), so a counter can never block a client forever.
     */
    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> HIT_SCRIPT = RedisScript.of("""
        local count = redis.call('INCR', KEYS[1])
        if count == 1 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
        end
        local ttl = redis.call('TTL', KEYS[1])
        if ttl < 0 then
          redis.call('EXPIRE', KEYS[1], ARGV[1])
          ttl = tonumber(ARGV[1])
        end
        return {count, ttl}
        """, List.class);

    private final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Records one hit on the counter identified by {@code key} and reports where the client stands
     * against {@code limit} hits per {@code window}.
     *
     * <p>Fails open: if Redis is unreachable the hit is allowed and the error logged, so a cache
     * outage never takes the API down.
     */
    public RateLimitResult hit(String key, long limit, Duration window) {
        long windowSeconds = window.toSeconds();

        try {
            @SuppressWarnings("unchecked")
            List<Long> reply =
                    stringRedisTemplate.execute(HIT_SCRIPT, List.of(KEY_PREFIX + key), String.valueOf(windowSeconds));

            // A null reply only happens inside a pipeline/transaction, which we never use here.
            if (reply == null || reply.size() < 2) {
                log.warn("Unexpected reply from rate-limit script for key {}: {}", key, reply);

                return new RateLimitResult(0, limit, windowSeconds);
            }

            return new RateLimitResult(reply.get(0), limit, reply.get(1));
        } catch (DataAccessException e) {
            log.error("Redis unavailable, rate limit skipped for key {}: {}", key, e.getMessage());

            return new RateLimitResult(0, limit, windowSeconds);
        }
    }

    /**
     * Writes a 429 Too Many Requests response with a Retry-After header.
     */
    public void writeRateLimitResponse(HttpServletResponse response, RateLimitResult result) throws IOException {
        long waitSeconds = Math.max(1, result.resetAfterSeconds());
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
     * the frontend server's IP and would otherwise exhaust a single counter): the request was not
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
}
