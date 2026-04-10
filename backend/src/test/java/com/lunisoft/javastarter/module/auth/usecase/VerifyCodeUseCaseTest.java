package com.lunisoft.javastarter.module.auth.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.JwtTokenProvider;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.AuthConstants;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VerifyCodeUseCaseTest {

  @Mock private AccountRepository accountRepository;
  @Mock private VerificationTokenRepository verificationTokenRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private HttpServletRequest request;

  private VerifyCodeUseCase verifyCodeUseCase;

  @BeforeEach
  void setUp() {
    verifyCodeUseCase =
        new VerifyCodeUseCase(
            accountRepository, verificationTokenRepository, sessionRepository, jwtTokenProvider);
  }

  @Test
  void execute_validCode_returnsAuthResponse() {
    var email = "test@example.com";
    var code = "1234";
    var account = createAccount(email);
    var token = createVerificationToken(account, code, 0);
    var session = createSession(account);

    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(account.getId()), eq(VerificationType.LOGIN_CODE), any(Instant.class)))
        .thenReturn(Optional.of(token));
    when(sessionRepository.save(any(Session.class))).thenReturn(session);
    when(jwtTokenProvider.generateAccessToken(
            session.getId(), account.getId(), email, Role.CUSTOMER))
        .thenReturn("access-token");
    when(jwtTokenProvider.generateRefreshToken(session.getId())).thenReturn("refresh-token");
    when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(10080);

    var result = verifyCodeUseCase.execute(email, code, request);

    assertThat(result.role()).isEqualTo("CUSTOMER");
    assertThat(result.accessToken()).isEqualTo("access-token");
    assertThat(result.refreshToken()).isEqualTo("refresh-token");
    verify(verificationTokenRepository).delete(token);
    verify(accountRepository).save(account);
    assertThat(account.isEmailVerified()).isTrue();
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    when(accountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> verifyCodeUseCase.execute("unknown@example.com", "1234", request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("INVALID_CODE");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            });
  }

  @Test
  void execute_noValidToken_throwsBusinessRuleException() {
    var email = "test@example.com";
    var account = createAccount(email);
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(account.getId()), eq(VerificationType.LOGIN_CODE), any(Instant.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> verifyCodeUseCase.execute(email, "1234", request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("INVALID_CODE");
            });
  }

  @Test
  void execute_maxAttemptsReached_throwsBusinessRuleException() {
    var email = "test@example.com";
    var account = createAccount(email);
    var token = createVerificationToken(account, "1234", AuthConstants.LOGIN_CODE_MAX_ATTEMPTS);

    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(account.getId()), eq(VerificationType.LOGIN_CODE), any(Instant.class)))
        .thenReturn(Optional.of(token));

    assertThatThrownBy(() -> verifyCodeUseCase.execute(email, "1234", request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("MAX_ATTEMPTS_REACHED");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            });
  }

  @Test
  void execute_wrongCode_incrementsAttemptsAndThrows() {
    var email = "test@example.com";
    var account = createAccount(email);
    var token = createVerificationToken(account, "1234", 0);

    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
            eq(account.getId()), eq(VerificationType.LOGIN_CODE), any(Instant.class)))
        .thenReturn(Optional.of(token));

    assertThatThrownBy(() -> verifyCodeUseCase.execute(email, "9999", request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("INVALID_CODE");
            });

    // Attempts should be incremented and saved
    assertThat(token.getAttempts()).isEqualTo(1);
    verify(verificationTokenRepository).save(token);
    // Token should NOT be deleted
    verify(verificationTokenRepository, never()).delete(any());
  }

}
