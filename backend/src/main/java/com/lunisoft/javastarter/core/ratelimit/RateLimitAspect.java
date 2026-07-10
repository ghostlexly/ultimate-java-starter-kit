package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.core.security.UserPrincipal;
import io.github.bucket4j.ConsumptionProbe;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Enforces @RateLimit via AOP. Because Spring resolves and validates controller arguments
 * (including @Valid @RequestBody) BEFORE invoking the method, this advice only runs for requests
 * that pass validation — invalid payloads do not consume a token.
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
        String key = buildKey(pjp, request);

        ConsumptionProbe probe =
                rateLimitService.tryConsume(key, rateLimit.requests(), Duration.ofSeconds(rateLimit.periodSeconds()));

        if (!probe.isConsumed()) {
            rateLimitService.writeRateLimitResponse(response, probe);

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

    /**
     * Builds a unique key per endpoint per client.
     */
    private String buildKey(ProceedingJoinPoint pjp, HttpServletRequest request) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        String endpoint = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        String clientId = resolveClientId(request);

        return endpoint + ":" + clientId;
    }

    /**
     * Returns "user:<accountId>" if authenticated, otherwise "ip:<address>".
     */
    private String resolveClientId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.accountId();
        }

        return "ip:" + rateLimitService.resolveIpAddress(request);
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
