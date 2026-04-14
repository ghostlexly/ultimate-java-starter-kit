package com.lunisoft.javastarter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration. Rate limiting is now applied via {@link
 * com.lunisoft.javastarter.core.ratelimit.RateLimitAspect} (AOP around the controller method)
 * instead of an interceptor, so invalid payloads don't consume tokens.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {}
