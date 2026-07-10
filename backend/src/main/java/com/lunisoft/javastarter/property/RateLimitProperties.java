package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate limiting configuration. The {@code global} limit is applied to every request per client IP
 * by {@link com.lunisoft.javastarter.core.ratelimit.GlobalRateLimitFilter}.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(Global global) {

    public record Global(int requests, int periodSeconds) {}
}
