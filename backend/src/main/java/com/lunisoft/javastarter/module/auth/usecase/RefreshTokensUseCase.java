package com.lunisoft.javastarter.module.auth.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.JwtTokenProvider;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.auth.dto.AuthResponse;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates the refresh token, extends the session, and returns new JWT tokens. */
@Service
@RequiredArgsConstructor
public class RefreshTokensUseCase {

  private final SessionRepository sessionRepository;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public AuthResponse execute(String refreshToken) {
    Claims claims;
    try {
      claims = jwtTokenProvider.parseToken(refreshToken);
    } catch (Exception _) {
      throw new BusinessRuleException(
          "Invalid refresh token.", "INVALID_TOKEN", HttpStatus.UNAUTHORIZED);
    }

    UUID sessionId = UUID.fromString(claims.getSubject());

    Session session =
        sessionRepository
            .findByIdAndExpiresAtAfter(sessionId, Instant.now())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Session expired.", "SESSION_EXPIRED", HttpStatus.UNAUTHORIZED));

    // Extend session expiry on refresh
    session.setExpiresAt(
        Instant.now()
            .plus(jwtTokenProvider.getRefreshTokenExpirationMinutes(), ChronoUnit.MINUTES));
    sessionRepository.save(session);

    Account account = session.getAccount();

    String newAccessToken =
        jwtTokenProvider.generateAccessToken(
            session.getId(), account.getId(), account.getEmail(), account.getRole());
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(session.getId());

    return new AuthResponse(account.getRole().name(), newAccessToken, newRefreshToken);
  }
}
