package com.lunisoft.javastarter.core.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * {@link org.springframework.security.core.Authentication} carrying a JWT.
 *
 * <p>Use {@link #unauthenticated(String)} when handing a raw token to the {@code
 * AuthenticationManager}, and {@link #authenticated(UserPrincipal, String, Collection)} to wrap a
 * verified principal returned from {@link JwtAuthenticationProvider}.
 *
 * <p>For the suppress warning: Auth tokens are never compared; matches Spring's own
 * UsernamePasswordAuthenticationToken.
 */
@SuppressWarnings("java:S2160")
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  private final String token;
  private final UserPrincipal principal;

  private JwtAuthenticationToken(
      UserPrincipal principal,
      String token,
      Collection<? extends GrantedAuthority> authorities,
      boolean authenticated) {
    super(authorities);
    this.token = token;
    this.principal = principal;
    super.setAuthenticated(authenticated);
  }

  public static JwtAuthenticationToken unauthenticated(String token) {
    return new JwtAuthenticationToken(null, token, null, false);
  }

  public static JwtAuthenticationToken authenticated(
      UserPrincipal principal, String token, Collection<? extends GrantedAuthority> authorities) {
    return new JwtAuthenticationToken(principal, token, authorities, true);
  }

  @Override
  public Object getCredentials() {
    return token;
  }

  @Override
  public Object getPrincipal() {
    return principal;
  }
}
