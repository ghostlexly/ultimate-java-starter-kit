package com.lunisoft.javastarter.module.auth.controller;

import com.lunisoft.javastarter.core.dto.MessageResponse;
import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.PublicEndpoint;
import com.lunisoft.javastarter.core.security.UserPrincipal;
import com.lunisoft.javastarter.module.auth.dto.RefreshTokenRequest;
import com.lunisoft.javastarter.module.auth.dto.SendCodeRequest;
import com.lunisoft.javastarter.module.auth.dto.VerifyCodeRequest;
import com.lunisoft.javastarter.module.auth.service.AuthCookieService;
import com.lunisoft.javastarter.module.auth.usecase.GetMeUseCase;
import com.lunisoft.javastarter.module.auth.usecase.GetMeUseCase.GetMeQuery;
import com.lunisoft.javastarter.module.auth.usecase.GetMeUseCase.GetMeResult;
import com.lunisoft.javastarter.module.auth.usecase.RefreshTokensUseCase;
import com.lunisoft.javastarter.module.auth.usecase.RefreshTokensUseCase.RefreshTokensCommand;
import com.lunisoft.javastarter.module.auth.usecase.RefreshTokensUseCase.RefreshTokensResult;
import com.lunisoft.javastarter.module.auth.usecase.SendCodeUseCase;
import com.lunisoft.javastarter.module.auth.usecase.SendCodeUseCase.SendCodeCommand;
import com.lunisoft.javastarter.module.auth.usecase.VerifyCodeUseCase;
import com.lunisoft.javastarter.module.auth.usecase.VerifyCodeUseCase.VerifyCodeCommand;
import com.lunisoft.javastarter.module.auth.usecase.VerifyCodeUseCase.VerifyCodeResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    @PostMapping("send-code")
    public ResponseEntity<MessageResponse> sendCode(@Valid @RequestBody SendCodeRequest request) {
        var command = new SendCodeCommand(request.email());
        this.sendCodeUseCase.execute(command);

        return ResponseEntity.ok(new MessageResponse("Login code sent successfully."));
    }

    @PublicEndpoint
    @PostMapping("verify-code")
    public ResponseEntity<VerifyCodeResult> verifyCode(
            @Valid @RequestBody VerifyCodeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        var command = new VerifyCodeCommand(request.email(), request.code(), httpRequest);
        var result = this.verifyCodeUseCase.execute(command);

        this.authCookieService.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(result);
    }

    @PublicEndpoint
    @PostMapping("refresh")
    public ResponseEntity<RefreshTokensResult> refreshTokens(
            @Valid @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String refreshToken = this.authCookieService.resolveRefreshToken(request, httpRequest);

        if (refreshToken == null) {
            throw new BusinessRuleException("Refresh token is required.", "MISSING_TOKEN", HttpStatus.BAD_REQUEST);
        }

        var command = new RefreshTokensCommand(refreshToken);
        var result = this.refreshTokensUseCase.execute(command);

        this.authCookieService.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(result);
    }

    @GetMapping("me")
    public ResponseEntity<GetMeResult> me(@AuthenticationPrincipal UserPrincipal principal) {
        var query = new GetMeQuery(principal.accountId());
        var result = this.getMeUseCase.execute(query);

        return ResponseEntity.ok(result);
    }

    @PostMapping("logout")
    public ResponseEntity<MessageResponse> logout(HttpServletResponse httpResponse) {
        this.authCookieService.clearAuthCookies(httpResponse);

        return ResponseEntity.ok(new MessageResponse("You have been successfully logged out."));
    }
}
