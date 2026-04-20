package com.lunisoft.javastarter.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts a JWT from the request (cookie or {@code Authorization} header) and delegates validation
 * to the {@link AuthenticationManager}. On success, the resulting {@link Authentication} is placed
 * in the {@link SecurityContextHolder}; on failure, the request continues unauthenticated and is
 * rejected later by {@code .anyRequest().authenticated()} via the configured entry point.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final AuthenticationManager authenticationManager;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null) {
      try {
        Authentication auth =
            authenticationManager.authenticate(JwtAuthenticationToken.unauthenticated(token));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
      } catch (AuthenticationException _) {
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
