package com.lunisoft.ultimatejavastarterkit.module.auth.controller;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.*;
import com.lunisoft.ultimatejavastarterkit.module.auth.usecase.GetMeUseCase;
import com.lunisoft.ultimatejavastarterkit.module.auth.usecase.RefreshTokensUseCase;
import com.lunisoft.ultimatejavastarterkit.module.auth.usecase.SendCodeUseCase;
import com.lunisoft.ultimatejavastarterkit.module.auth.usecase.VerifyCodeUseCase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Value("${app.cookie.secure:false}")
  private boolean secureCookies;

  private final SendCodeUseCase sendCodeUseCase;
  private final VerifyCodeUseCase verifyCodeUseCase;
  private final RefreshTokensUseCase refreshTokensUseCase;
  private final GetMeUseCase getMeUseCase;

  public AuthController(
      SendCodeUseCase sendCodeUseCase,
      VerifyCodeUseCase verifyCodeUseCase,
      RefreshTokensUseCase refreshTokensUseCase,
      GetMeUseCase getMeUseCase) {
    this.sendCodeUseCase = sendCodeUseCase;
    this.verifyCodeUseCase = verifyCodeUseCase;
    this.refreshTokensUseCase = refreshTokensUseCase;
    this.getMeUseCase = getMeUseCase;
  }

  @PostMapping("/send-code")
  public ResponseEntity<Map<String, String>> sendCode(@Valid @RequestBody SendCodeRequest request) {
    sendCodeUseCase.execute(request.email());

    return ResponseEntity.ok(Map.of("message", "Login code sent successfully."));
  }

  @PostMapping("/verify-code")
  public ResponseEntity<AuthResponse> verifyCode(
      @Valid @RequestBody VerifyCodeRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    AuthResponse response = verifyCodeUseCase.execute(request.email(), request.code(), httpRequest);
    setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshTokens(
      @Valid @RequestBody(required = false) RefreshTokenRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    String refreshToken = resolveRefreshToken(request, httpRequest);

    if (refreshToken == null) {
      throw new BusinessRuleException(
          "Refresh token is required.", "MISSING_TOKEN", HttpStatus.BAD_REQUEST);
    }

    AuthResponse response = refreshTokensUseCase.execute(refreshToken);
    setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
    MeResponse response = getMeUseCase.execute(principal.accountId());

    return ResponseEntity.ok(response);
  }

  private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
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

  private void setAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
    addCookie(response, "lunisoft_access_token", authResponse.accessToken(), 15 * 60);
    addCookie(response, "lunisoft_refresh_token", authResponse.refreshToken(), 7 * 24 * 60 * 60);
  }

  private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(secureCookies);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    response.addCookie(cookie);
  }
}
