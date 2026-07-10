package com.lunisoft.javastarter.module.auth.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.core.security.JwtTokenProvider;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static com.lunisoft.javastarter.shared.TestFactory.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokensUseCaseTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Claims claims;

    @InjectMocks
    private RefreshTokensUseCase refreshTokensUseCase;

    @Test
    void execute_valid_token_returns_new_tokens() {
        String refreshToken = "valid-refresh-token";
        Account account = createCustomerAccount();
        var session = createSession(account);
        var sessionId = session.getId();

        when(jwtTokenProvider.parseToken(refreshToken)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(sessionId.toString());
        when(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any(Instant.class)))
                .thenReturn(Optional.of(session));
        when(jwtTokenProvider.getRefreshTokenExpirationMinutes()).thenReturn(10080);
        when(jwtTokenProvider.generateAccessToken(sessionId, account.getId(), account.getEmail(), Role.CUSTOMER))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(sessionId)).thenReturn("new-refresh-token");

        var output = refreshTokensUseCase.execute(refreshToken);

        assertThat(output.role()).isEqualTo("CUSTOMER");
        assertThat(output.accessToken()).isEqualTo("new-access-token");
        assertThat(output.refreshToken()).isEqualTo("new-refresh-token");

        // Session expiry should be extended
        assertThat(session.getExpiresAt())
                .isAfter(Instant.now()
                        .plus(jwtTokenProvider.getRefreshTokenExpirationMinutes() - 1, ChronoUnit.MINUTES));
    }

    @Test
    void execute_invalid_token_throws_business_rule_exception() {
        when(jwtTokenProvider.parseToken("bad-token")).thenThrow(new RuntimeException("Invalid"));

        assertThatThrownBy(() -> refreshTokensUseCase.execute("bad-token"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    var bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("INVALID_TOKEN");
                    assertThat(bre.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void execute_expired_session_throws_business_rule_exception() {
        var sessionId = UUID.randomUUID();

        when(jwtTokenProvider.parseToken("expired-session-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(sessionId.toString());
        when(sessionRepository.findByIdAndExpiresAtAfter(eq(sessionId), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokensUseCase.execute("expired-session-token"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    var bre = (BusinessRuleException) ex;
                    assertThat(bre.getCode()).isEqualTo("SESSION_EXPIRED");
                    assertThat(bre.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }
}
