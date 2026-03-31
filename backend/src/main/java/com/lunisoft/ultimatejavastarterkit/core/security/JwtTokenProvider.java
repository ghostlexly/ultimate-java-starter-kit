package com.lunisoft.ultimatejavastarterkit.core.security;

import com.lunisoft.ultimatejavastarterkit.config.JwtProperties;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/** Handles JWT token generation and validation using RSA256. */
@Component
public class JwtTokenProvider {

  private final PrivateKey privateKey;
  private final PublicKey publicKey;
  private final JwtProperties jwtProperties;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.privateKey = decodePrivateKey(jwtProperties.privateKey());
    this.publicKey = decodePublicKey(jwtProperties.publicKey());
  }

  /** Generates a short-lived access token containing user identity and role. */
  public String generateAccessToken(UUID sessionId, UUID accountId, String email, Role role) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(accountId.toString())
        .claim("sessionId", sessionId.toString())
        .claim("email", email)
        .claim("role", role.name())
        .issuedAt(Date.from(now))
        .expiration(
            Date.from(now.plus(jwtProperties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES)))
        .signWith(privateKey)
        .compact();
  }

  /** Generates a long-lived refresh token tied to a session. */
  public String generateRefreshToken(UUID sessionId) {
    Instant now = Instant.now();

    return Jwts.builder()
        .subject(sessionId.toString())
        .issuedAt(Date.from(now))
        .expiration(
            Date.from(now.plus(jwtProperties.refreshTokenExpirationMinutes(), ChronoUnit.MINUTES)))
        .signWith(privateKey)
        .compact();
  }

  public int getRefreshTokenExpirationMinutes() {
    return jwtProperties.refreshTokenExpirationMinutes();
  }

  /** Parses and validates a JWT token, returning its claims. */
  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }

  private PrivateKey decodePrivateKey(String base64Key) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      String pem =
          new String(keyBytes)
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replace("-----BEGIN RSA PRIVATE KEY-----", "")
              .replace("-----END RSA PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(pem);

      return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode JWT private key", e);
    }
  }

  private PublicKey decodePublicKey(String base64Key) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64Key);
      String pem =
          new String(keyBytes)
              .replace("-----BEGIN PUBLIC KEY-----", "")
              .replace("-----END PUBLIC KEY-----", "")
              .replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(pem);

      return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decode JWT public key", e);
    }
  }
}
