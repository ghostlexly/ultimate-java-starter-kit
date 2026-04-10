package com.lunisoft.javastarter.module.auth.controller;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.security.UserPrincipal;
import com.lunisoft.javastarter.module.auth.dto.*;
import com.lunisoft.javastarter.module.auth.service.AuthCookieService;
import com.lunisoft.javastarter.module.auth.usecase.GetMeUseCase;
import com.lunisoft.javastarter.module.auth.usecase.RefreshTokensUseCase;
import com.lunisoft.javastarter.module.auth.usecase.SendCodeUseCase;
import com.lunisoft.javastarter.module.auth.usecase.VerifyCodeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthCookieService authCookieService;
  private final SendCodeUseCase sendCodeUseCase;
  private final VerifyCodeUseCase verifyCodeUseCase;
  private final RefreshTokensUseCase refreshTokensUseCase;
  private final GetMeUseCase getMeUseCase;

  @PublicEndpoint
  @PostMapping("/send-code")
  public ResponseEntity<Map<String, String>> sendCode(@Valid @RequestBody SendCodeRequest request) {
    sendCodeUseCase.execute(request.email());

    return ResponseEntity.ok(Map.of("message", "Login code sent successfully."));
  }

  @PublicEndpoint
  @PostMapping("/verify-code")
  public ResponseEntity<AuthResponse> verifyCode(
      @Valid @RequestBody VerifyCodeRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    AuthResponse response = verifyCodeUseCase.execute(request.email(), request.code(), httpRequest);
    authCookieService.setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @PublicEndpoint
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshTokens(
      @Valid @RequestBody(required = false) RefreshTokenRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    String refreshToken = authCookieService.resolveRefreshToken(request, httpRequest);

    if (refreshToken == null) {
      throw new BusinessRuleException(
          "Refresh token is required.", "MISSING_TOKEN", HttpStatus.BAD_REQUEST);
    }

    AuthResponse response = refreshTokensUseCase.execute(refreshToken);
    authCookieService.setAuthCookies(httpResponse, response);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
    MeResponse response = getMeUseCase.execute(principal.accountId());

    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, String>> logout(HttpServletResponse httpResponse) {
    authCookieService.clearAuthCookies(httpResponse);

    return ResponseEntity.ok(Map.of("message", "You have been successfully logged out"));
  }
}
