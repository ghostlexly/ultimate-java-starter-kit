package com.lunisoft.ultimatejavastarterkit.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits the number of requests to an endpoint within a time window.
 * Uses IP address for unauthenticated requests, user ID for authenticated ones.
 *
 * <p>Example: @RateLimit(requests = 5, periodSeconds = 60) — 5 requests per minute.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
  int requests();
  int periodSeconds();
}
