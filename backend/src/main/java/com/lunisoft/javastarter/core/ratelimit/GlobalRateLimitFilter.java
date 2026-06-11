package com.lunisoft.javastarter.core.ratelimit;

import com.lunisoft.javastarter.property.RateLimitProperties;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Global safety net protecting the whole application from abuse: limits every request to a maximum
 * per period per client IP (100/min by default, see {@code app.rate-limit.global}).
 *
 * <p>Registered with the highest precedence so it runs before the Spring Security filter chain —
 * authentication endpoints and rejected requests are throttled too. Stricter per-endpoint limits
 * (e.g. login) are still enforced on top via {@link RateLimit}.
 *
 * <p>Trusted server-to-server traffic (Next.js SSR, identified by a private source address with
 * no proxy forwarding headers) bypasses the limit: all SSR requests reach us with the frontend
 * server's IP and would otherwise exhaust a single bucket on behalf of every visitor.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GlobalRateLimitFilter extends OncePerRequestFilter {

  private final RateLimitService rateLimitService;
  private final RateLimitProperties rateLimitProperties;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Trusted internal traffic (Next.js SSR) is exempt from the global per-IP limit.
    if (rateLimitService.isTrustedInternalRequest(request)) {
      filterChain.doFilter(request, response);

      return;
    }

    String key = "global:ip:%s".formatted(rateLimitService.resolveIpAddress(request));
    ConsumptionProbe probe =
        rateLimitService.tryConsume(
            key,
            rateLimitProperties.global().requests(),
            Duration.ofSeconds(rateLimitProperties.global().periodSeconds()));

    if (!probe.isConsumed()) {
      // Short-circuit the chain — the request never reaches Spring Security or a controller.
      rateLimitService.writeRateLimitResponse(response, probe);

      return;
    }

    filterChain.doFilter(request, response);
  }
}
