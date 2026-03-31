package com.lunisoft.ultimatejavastarterkit.core.security;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts and validates JWT from cookie or Authorization header. Sets the SecurityContext
 * authentication if the token is valid and the session exists.
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final SessionRepository sessionRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null) {
      try {
        Claims claims = jwtTokenProvider.parseToken(token);
        UUID accountId = UUID.fromString(claims.getSubject());
        UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        // Verify session is still valid (exists and not expired)
        if (sessionRepository.existsByIdAndExpiresAtAfter(sessionId, Instant.now())) {
          UserPrincipal principal =
              new UserPrincipal(accountId, email, Role.valueOf(role), sessionId);
          UsernamePasswordAuthenticationToken auth =
              new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      } catch (Exception _) {
        // Invalid token — continue unauthenticated
      }
    }

    filterChain.doFilter(request, response);
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
