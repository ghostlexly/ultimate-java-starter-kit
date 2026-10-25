package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Rate limiting configuration. The {@code global} limit is applied to every request per client IP
 * by {@link com.lunisoft.javastarter.core.ratelimit.GlobalRateLimitFilter}.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Global global) {

    /**
     * @param requests      maximum number of requests per window and per client IP
     * @param period        length of the fixed window ("30s", "5m", "1h" or ISO-8601 "PT1H")
     * @param message       429 body message; supports {@code {minutes}} / {@code {seconds}} placeholders,
     *                      blank or absent falls back to the service default
     */
    public record Global(int requests, Duration period, String message) {}
}
