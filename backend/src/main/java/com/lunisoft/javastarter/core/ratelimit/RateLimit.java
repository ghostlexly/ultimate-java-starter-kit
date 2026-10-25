package com.lunisoft.javastarter.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits the number of requests to an endpoint within a fixed time window.
 *
 * <p>The quota is enforced on every client dimension at once: the client IP always, plus the
 * account email when the request is authenticated. One IP therefore cannot multiply its quota by
 * rotating accounts, and one account cannot multiply it by rotating IPs.
 *
 * <p>Example: @RateLimit(requests = 5, window = "1m") — 5 requests per minute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Counter name. Endpoints annotated with the same name share one quota (e.g. every kind of
     * scan counts against a single "scan" budget). Defaults to the endpoint itself.
     */
    String name() default "";

    /** Maximum number of requests allowed per window. */
    int requests();

    /**
     * Length of the fixed window, in the same format Spring Boot accepts for {@code Duration}
     * properties: a simple value with unit ("30s", "5m", "1h", "1d") or ISO-8601 ("PT1H").
     */
    String window();

    /**
     * User-facing message returned in the 429 body when the quota is exceeded. Supports the
     * {@code {minutes}} and {@code {seconds}} placeholders, replaced by the time left before the
     * window resets (minutes is never below 1). Blank falls back to
     * {@link RateLimitService#DEFAULT_MESSAGE}.
     */
    String message() default "";
}
