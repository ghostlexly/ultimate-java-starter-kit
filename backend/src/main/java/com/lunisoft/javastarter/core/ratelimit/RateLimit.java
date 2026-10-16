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
 * <p>Example: @RateLimit(requests = 5, periodSeconds = 60) — 5 requests per minute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Counter name. Endpoints annotated with the same name share one quota (e.g. every kind of
     * scan counts against a single "scan" budget). Defaults to the endpoint itself.
     */
    String name() default "";

    int requests();

    int periodSeconds();
}
