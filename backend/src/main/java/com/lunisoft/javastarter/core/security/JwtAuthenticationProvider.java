package com.lunisoft.javastarter.core.security;

import com.lunisoft.javastarter.module.account.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link JwtAuthenticationToken}: parses the JWT and returns a fully authenticated
 * token populated with the {@link UserPrincipal}.
 *
 * <p>This provider deliberately does <b>not</b> hit the database to verify the underlying session.
 * Access tokens are short-lived; the session is checked only when refreshing tokens (see {@code
 * RefreshTokensUseCase}). This trade-off keeps every authenticated request a pure CPU operation
 * (signature verification + claim parsing), at the cost of being unable to revoke an issued access
 * token before its natural expiry.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationProvider.class);

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String token = (String) authentication.getCredentials();

    try {
      Claims claims = jwtTokenProvider.parseToken(token);
      UUID accountId = UUID.fromString(claims.getSubject());
      UUID sessionId = UUID.fromString(claims.get("sessionId", String.class));
      String email = claims.get("email", String.class);
      String role = claims.get("role", String.class);

      UserPrincipal principal = new UserPrincipal(accountId, email, Role.valueOf(role), sessionId);

      return JwtAuthenticationToken.authenticated(principal, token, principal.getAuthorities());
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("JWT authentication failed: {}", e.getClass().getSimpleName());

      throw new BadCredentialsException("Invalid JWT token");
    }
  }

  @Override
  public boolean supports(@NonNull Class<?> authentication) {
    return JwtAuthenticationToken.class.isAssignableFrom(authentication);
  }
}
