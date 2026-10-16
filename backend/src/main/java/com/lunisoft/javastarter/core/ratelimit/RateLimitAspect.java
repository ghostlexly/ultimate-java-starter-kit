package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.core.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Enforces @RateLimit via AOP. Because Spring resolves and validates controller arguments
 * (including @Valid @RequestBody) BEFORE invoking the method, this advice only runs for requests
 * that pass validation — invalid payloads do not count against the quota.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();
        String limitName = resolveLimitName(pjp, rateLimit);
        Duration window = Duration.ofSeconds(rateLimit.periodSeconds());

        // Every client dimension is counted, so the quota holds per IP AND per account.
        List<RateLimitResult> results = resolveClientKeys(request).stream()
                .map(clientKey -> rateLimitService.hit(
                        "%s:%s".formatted(limitName, clientKey), rateLimit.requests(), window))
                .toList();

        Optional<RateLimitResult> exceeded =
                results.stream().filter(RateLimitResult::isExceeded).findFirst();

        if (exceeded.isPresent()) {
            rateLimitService.writeRateLimitResponse(response, exceeded.get());

            // Short-circuit the controller — Spring will not write another body
            // because the response is already committed.
            return null;
        }

        long remaining = results.stream()
                .mapToLong(RateLimitResult::remaining)
                .min()
                .orElse(rateLimit.requests());
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remaining));

        // Proceed with the controller method; if it throws, the hit stays counted
        // (business failures still count against the limit — only arg-resolution
        // failures like @Valid never reach this advice).
        return pjp.proceed();
    }

    /**
     * Explicit {@link RateLimit#name()} when given, otherwise "Class#method" so each endpoint
     * gets its own counter.
     */
    private String resolveLimitName(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        if (StringUtils.hasText(rateLimit.name())) {
            return rateLimit.name();
        }

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }

    /**
     * Returns "ip:<address>" always, plus "email:<email>" when the request is authenticated.
     */
    private List<String> resolveClientKeys(HttpServletRequest request) {
        List<String> clientKeys = new ArrayList<>();
        clientKeys.add("ip:" + rateLimitService.resolveIpAddress(request));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal && principal.email() != null) {
            clientKeys.add("email:" + principal.email().toLowerCase(Locale.ROOT));
        }

        return clientKeys;
    }

    private HttpServletRequest currentRequest() {
        return currentAttrs().getRequest();
    }

    private HttpServletResponse currentResponse() {
        HttpServletResponse response = currentAttrs().getResponse();

        if (response == null) {
            throw new IllegalStateException("No current HTTP response — @RateLimit requires a servlet context");
        }

        return response;
    }

    private ServletRequestAttributes currentAttrs() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs == null) {
            throw new IllegalStateException("No current HTTP request — @RateLimit requires a servlet context");
        }

        return attrs;
    }
}
