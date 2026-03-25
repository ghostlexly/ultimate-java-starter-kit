package com.lunisoft.ultimatejavastarterkit.module.auth;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.security.JwtTokenProvider;
import com.lunisoft.ultimatejavastarterkit.entity.*;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.AuthResponse;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.MeResponse;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.RefreshTokenRequest;
import com.lunisoft.ultimatejavastarterkit.module.auth.event.LoginCodeRequestedEvent;
import com.lunisoft.ultimatejavastarterkit.module.auth.event.LoginCodeRequestedListener;
import com.lunisoft.ultimatejavastarterkit.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.repository.SessionRepository;
import com.lunisoft.ultimatejavastarterkit.repository.VerificationTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  @Value("${app.cookie.secure:false}")
  private boolean secureCookies;

  private final AccountRepository accountRepository;
  private final SessionRepository sessionRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final ApplicationEventPublisher eventPublisher;

  public AuthService(
      AccountRepository accountRepository,
      SessionRepository sessionRepository,
      VerificationTokenRepository verificationTokenRepository,
      JwtTokenProvider jwtTokenProvider,
      ApplicationEventPublisher eventPublisher) {
    this.accountRepository = accountRepository;
    this.sessionRepository = sessionRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.jwtTokenProvider = jwtTokenProvider;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Generates a 4-digit login code and stores it as a VerificationToken. Creates the account if it
   * doesn't exist (defaults to CUSTOMER role).
   */
  @Transactional
  public void sendCode(String email) {
    Account account =
        accountRepository
            .findByEmail(email)
            .orElseGet(
                () -> {
                  Account newAccount = new Account();
                  newAccount.setEmail(email);
                  newAccount.setRole(Role.CUSTOMER);

                  return accountRepository.save(newAccount);
                });

    // Enforce cooldown between code requests
    verificationTokenRepository
        .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE)
        .ifPresent(
            lastToken -> {
              long secondsSince =
                  Duration.between(lastToken.getCreatedAt(), Instant.now()).toSeconds();
              if (secondsSince < AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS) {
                long remaining = AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS - secondsSince;
                throw new BusinessRuleException(
                    "Please wait " + remaining + " seconds before requesting a new code.",
                    "LOGIN_CODE_COOLDOWN",
                    HttpStatus.TOO_MANY_REQUESTS);
              }
            });

    String code = String.format("%04d", new SecureRandom().nextInt(10000));

    VerificationToken token = new VerificationToken();
    token.setToken(UUID.randomUUID().toString());
    token.setType(VerificationType.LOGIN_CODE);
    token.setValue(code);
    token.setAccount(account);
    token.setExpiresAt(
        Instant.now().plus(AuthConstants.LOGIN_CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
    verificationTokenRepository.save(token);

    eventPublisher.publishEvent(new LoginCodeRequestedEvent(email, code));
  }

  /**
   * Validates the login code, creates a session and returns JWT tokens. Includes brute-force
   * protection (max 5 attempts per code).
   */
  @Transactional(noRollbackFor = BusinessRuleException.class)
  public AuthResponse verifyCode(String email, String code, HttpServletRequest request) {
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

  /** Validates the refresh token, extends the session, and returns new JWT tokens. */
  @Transactional
  public AuthResponse refreshTokens(String refreshToken) {
    Claims claims;
    try {
      claims = jwtTokenProvider.parseToken(refreshToken);
    } catch (Exception e) {
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

  /** Returns the current authenticated user's info. */
  public MeResponse me(UUID accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

    return new MeResponse(account.getId(), account.getEmail(), account.getRole().name());
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

  public String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
    // Check request body first
    if (request != null && request.refreshToken() != null) {
      return request.refreshToken();
    }

    // Fallback to cookie
    if (httpRequest.getCookies() != null) {
      for (Cookie cookie : httpRequest.getCookies()) {
        if ("lunisoft_refresh_token".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }

    return null;
  }

  public void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
    addCookie(response, "lunisoft_access_token", authResponse.accessToken(), 15 * 60);
    addCookie(response, "lunisoft_refresh_token", authResponse.refreshToken(), 7 * 24 * 60 * 60);
  }

  public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(secureCookies);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    response.addCookie(cookie);
  }
}
