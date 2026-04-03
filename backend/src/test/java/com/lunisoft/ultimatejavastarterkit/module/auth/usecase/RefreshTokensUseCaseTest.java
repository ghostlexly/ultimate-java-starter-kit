package com.lunisoft.ultimatejavastarterkit.module.auth.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.security.JwtTokenProvider;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.Session;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RefreshTokensUseCaseTest {

  @Mock private SessionRepository sessionRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private Claims claims;

  private RefreshTokensUseCase refreshTokensUseCase;

  @BeforeEach
  void setUp() {
    refreshTokensUseCase = new RefreshTokensUseCase(sessionRepository, jwtTokenProvider);
  }

  @Test
  void execute_validToken_returnsNewTokens() {
    var refreshToken = "valid-refresh-token";
    var sessionId = UUID.randomUUID();
    var account = createAccount();
    var session = createSession(sessionId, account);

    when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
    when(claims.getSubject()).thenReturn(sessionId.toString());
    when(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any(Instant.class)))
        .thenReturn(Optional.of(session));
    when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(10080);
    when(jwtTokenProvider.generateAccessToken(
            sessionId, account.getId(), account.getEmail(), Role.CUSTOMER))
        .thenReturn("new-access-token");
    when(jwtTokenProvider.generateRefreshToken(sessionId)).thenReturn("new-refresh-token");

    var result = refreshTokensUseCase.execute(refreshToken);

    assertThat(result.role()).isEqualTo("CUSTOMER");
    assertThat(result.accessToken()).isEqualTo("new-access-token");
    assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    // Session expiry should be extended
    verify(sessionRepository).save(session);
  }

  @Test
  void execute_invalidToken_throwsBusinessRuleException() {
    when(jwtTokenProvider.parseToken("bad-token")).thenThrow(new RuntimeException("Invalid"));

    assertThatThrownBy(() -> refreshTokensUseCase.execute("bad-token"))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("INVALID_TOKEN");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
  }

  @Test
  void execute_expiredSession_throwsBusinessRuleException() {
    var sessionId = UUID.randomUUID();

    when(jwtTokenProvider.parseToken("expired-session-token")).thenReturn(claims);
    when(claims.getSubject()).thenReturn(sessionId.toString());
    when(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any(Instant.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokensUseCase.execute("expired-session-token"))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("SESSION_EXPIRED");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
  }

  private Account createAccount() {
    var account = new Account();
    account.setId(UUID.randomUUID());
    account.setEmail("test@example.com");
    account.setRole(Role.CUSTOMER);

    return account;
  }

  private Session createSession(UUID sessionId, Account account) {
    var session = new Session();
    session.setId(sessionId);
    session.setAccount(account);
    session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

    return session;
  }
}
