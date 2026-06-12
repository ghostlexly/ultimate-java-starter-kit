package com.lunisoft.javastarter.core.security;

import com.lunisoft.javastarter.module.account.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts a JWT from the request (cookie or {@code Authorization} header), validates it, and on
 * success places an authenticated {@link Authentication} carrying the {@link UserPrincipal} in the
 * {@link SecurityContextHolder}. On failure, the request continues unauthenticated and is rejected
 * later by {@code .anyRequest().authenticated()} via the configured entry point.
 *
 * <p>Validation deliberately does <b>not</b> hit the database to verify the underlying session.
 * Access tokens are short-lived; the session is checked only when refreshing tokens (see {@code
 * RefreshTokensUseCase}). This trade-off keeps every authenticated request a pure CPU operation
 * (signature verification + claim parsing), at the cost of being unable to revoke an issued access
 * token before its natural expiry.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null) {
      try {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticate(token));
        SecurityContextHolder.setContext(context);
      } catch (JwtException | IllegalArgumentException e) {
        // Invalid token — continue unauthenticated
        log.debug("JWT authentication failed: {}", e.getClass().getSimpleName());
      }
    }

    filterChain.doFilter(request, response);
  }

  /** Parses and validates the JWT, returning an authenticated token with the user principal. */
  private Authentication authenticate(String token) {
    Claims claims = jwtTokenProvider.parseToken(token);
    UUID accountId = UUID.fromString(claims.getSubject());
    UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
    String email = claims.get("email", String.class);
    String role = claims.get("role", String.class);

    UserPrincipal principal = new UserPrincipal(accountId, email, Role.valueOf(role), sessionId);

    return UsernamePasswordAuthenticationToken.authenticated(
        principal, null, principal.getAuthorities());
  }

  /** Reads JWT from cookie (lunisoft_access_token) or Authorization header (Bearer ...). */
  private String resolveToken(HttpServletRequest request) {
    if (request.getCookies() != null) {
      for (Cookie cookie : request.getCookies()) {
        if ("lunisoft_access_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    String bearer = request.getHeader("Authorization");
    if (bearer != null && bearer.startsWith("Bearer ")) {
      return bearer.substring(7);
    }

    return null;
  }
}
