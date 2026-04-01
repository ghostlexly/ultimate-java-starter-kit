package com.lunisoft.ultimatejavastarterkit.module.auth.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.security.JwtTokenProvider;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.auth.AuthConstants;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.AuthResponse;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.Session;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.VerificationToken;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.VerificationType;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.SessionRepository;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.VerificationTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Validates the login code, creates a session and returns JWT tokens. Includes brute-force
 * protection (max 5 attempts per code).
 */
@Service
public class VerifyCodeUseCase {

  private final AccountRepository accountRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final SessionRepository sessionRepository;
  private final JwtTokenProvider jwtTokenProvider;

  public VerifyCodeUseCase(
      AccountRepository accountRepository,
      VerificationTokenRepository verificationTokenRepository,
      SessionRepository sessionRepository,
      JwtTokenProvider jwtTokenProvider) {
    this.accountRepository = accountRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.sessionRepository = sessionRepository;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Transactional(noRollbackFor = BusinessRuleException.class)
  public AuthResponse execute(String email, String code, HttpServletRequest request) {
    Account account =
        accountRepository
            .findByEmail(email)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Invalid email or code.", "INVALID_CODE", HttpStatus.BAD_REQUEST));

    VerificationToken token =
        verificationTokenRepository
            .findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
                account.getId(), VerificationType.LOGIN_CODE, Instant.now())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Invalid or expired code.", "INVALID_CODE", HttpStatus.BAD_REQUEST));

    if (token.getAttempts() >= AuthConstants.LOGIN_CODE_MAX_ATTEMPTS) {
      throw new BusinessRuleException(
          "Too many attempts. Please request a new code.",
          "MAX_ATTEMPTS_REACHED",
          HttpStatus.TOO_MANY_REQUESTS);
    }

    token.setAttempts(token.getAttempts() + 1);
    verificationTokenRepository.save(token);

    if (!code.equals(token.getValue())) {
      throw new BusinessRuleException("Invalid code.", "INVALID_CODE", HttpStatus.BAD_REQUEST);
    }

    // Code is valid — clean up and create session
    verificationTokenRepository.delete(token);

    account.setEmailVerified(true);
    accountRepository.save(account);

    Session session = createSession(account, request);

    String accessToken =
        jwtTokenProvider.generateAccessToken(
            session.getId(), account.getId(), account.getEmail(), account.getRole());
    String refreshToken = jwtTokenProvider.generateRefreshToken(session.getId());

    return new AuthResponse(account.getRole().name(), accessToken, refreshToken);
  }

  private Session createSession(Account account, HttpServletRequest request) {
    Session session = new Session();
    session.setAccount(account);
    session.setIpAddress(request.getRemoteAddr());
    session.setUserAgent(request.getHeader("User-Agent"));
    session.setExpiresAt(
        Instant.now()
            .plus(jwtTokenProvider.getRefreshTokenExpirationMinutes(), ChronoUnit.MINUTES));

    return sessionRepository.save(session);
  }
}
