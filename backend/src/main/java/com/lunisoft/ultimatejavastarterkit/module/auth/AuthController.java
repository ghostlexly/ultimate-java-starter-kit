package com.lunisoft.ultimatejavastarterkit.module.auth;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.core.security.UserPrincipal;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/send-code")
  public ResponseEntity<Map<String, String>> sendCode(@Valid @RequestBody SendCodeRequest request) {
    authService.sendCode(request.email());

    return ResponseEntity.ok(Map.of("message", "Login code sent successfully."));
  }

  @PostMapping("/verify-code")
  public ResponseEntity<AuthResponse> verifyCode(
      @Valid @RequestBody VerifyCodeRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    AuthResponse response = authService.verifyCode(request.email(), request.code(), httpRequest);
    authService.setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      @RequestBody(required = false) RefreshTokenRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    String refreshToken = authService.resolveRefreshToken(request, httpRequest);
    if (refreshToken == null) {
      throw new BusinessRuleException(
          "Refresh token is required.", "MISSING_TOKEN", HttpStatus.BAD_REQUEST);
    }

    AuthResponse response = authService.refreshTokens(refreshToken);
    authService.setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
    MeResponse response = authService.me(principal.accountId());

    return ResponseEntity.ok(response);
  }
}
