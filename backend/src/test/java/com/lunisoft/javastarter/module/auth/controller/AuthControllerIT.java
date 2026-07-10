package com.lunisoft.javastarter.module.auth.controller;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integration tests for {@link AuthController}. */
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private SessionRepository sessionRepository;

    /** POST /api/auth/send-code */
    @Nested
    class SendCode {

        private static final String URL = "/api/auth/send-code";

        @Test
        void creates_account_and_verification_token_for_new_email() throws Exception {
            var email = "new-user@example.com";
            var body = jsonMapper.writeValueAsString(Map.of("email", email));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Login code sent successfully."));

            var account = accountRepository.findByEmail(email).orElseThrow();
            assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(customerRepository.findByAccountId(account.getId())).isPresent();

            var token = verificationTokenRepository
                    .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(account.getId(), VerificationType.LOGIN_CODE)
                    .orElseThrow();
            assertThat(token.getValue()).hasSize(4).containsOnlyDigits();
        }

        @Test
        void returns_400_when_email_is_invalid() throws Exception {
            var body = jsonMapper.writeValueAsString(Map.of("email", "not-an-email"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns_429_when_requested_within_cooldown() throws Exception {
            var account = fixtures.givenCustomer("cooldown-user@example.com");
            fixtures.givenLoginCode(account, "1234");

            var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail()));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("LOGIN_CODE_COOLDOWN"));
        }
    }

    /** POST /api/auth/verify-code */
    @Nested
    class VerifyCode {

        private static final String URL = "/api/auth/verify-code";

        @Test
        void returns_tokens_and_sets_cookies_on_valid_code() throws Exception {
            var account = fixtures.givenCustomer("verify-success@example.com");
            fixtures.givenLoginCode(account, "4321");

            var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "4321"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(cookie().exists("lunisoft_access_token"))
                    .andExpect(cookie().exists("lunisoft_refresh_token"))
                    .andExpect(cookie().httpOnly("lunisoft_access_token", true));

            // Code is consumed exactly once.
            assertThat(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
                            account.getId(), VerificationType.LOGIN_CODE))
                    .isEmpty();
            // A real session row was persisted.
            assertThat(sessionRepository.count()).isEqualTo(1);
        }

        @Test
        void returns_400_on_wrong_code() throws Exception {
            var account = fixtures.givenCustomer("wrong-code@example.com");
            fixtures.givenLoginCode(account, "1111");

            var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "9999"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CODE"));

            // Wrong attempts must still be counted even though the transaction "fails".
            var token = verificationTokenRepository
                    .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(account.getId(), VerificationType.LOGIN_CODE)
                    .orElseThrow();
            assertThat(token.getAttempts()).isEqualTo(1);
        }

        @Test
        void returns_429_when_max_attempts_reached() throws Exception {
            var account = fixtures.givenCustomer("max-attempts@example.com");
            fixtures.givenLoginCode(account, "1111", t -> t.setAttempts(5));

            var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "1111"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("MAX_ATTEMPTS_REACHED"));
        }

        @Test
        void returns_400_when_code_expired() throws Exception {
            var account = fixtures.givenCustomer("expired-code@example.com");
            fixtures.givenLoginCode(
                    account, "2222", t -> t.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)));

            var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "2222"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CODE"));
        }
    }

    /** POST /api/auth/refresh */
    @Nested
    class RefreshTokens {

        private static final String URL = "/api/auth/refresh";

        @Test
        void returns_new_tokens_given_valid_refresh_token() throws Exception {
            var account = fixtures.givenCustomer("refresh@example.com");
            var session = fixtures.givenSession(account);
            var refreshToken = jwtTokenProvider.generateRefreshToken(session.getId());

            var body = jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        void returns_400_when_token_missing() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MISSING_TOKEN"));
        }

        @Test
        void returns_401_when_token_is_garbage() throws Exception {
            var body = jsonMapper.writeValueAsString(Map.of("refreshToken", "not-a-real-jwt"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }
    }

    /** GET /api/auth/me */
    @Nested
    class GetMe {

        private static final String URL = "/api/auth/me";

        @Test
        void returns_401_when_unauthenticated() throws Exception {
            mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        }

        @Test
        void returns_authenticated_user_when_jwt_is_valid() throws Exception {
            var account = fixtures.givenCustomer("me@example.com");

            mockMvc.perform(get(URL).header("Authorization", bearer(account)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
                    .andExpect(jsonPath("$.email").value(account.getEmail()))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }
    }

    /** POST /api/auth/logout */
    @Nested
    class Logout {

        private static final String URL = "/api/auth/logout";

        @Test
        void clears_auth_cookies() throws Exception {
            var account = fixtures.givenCustomer("logout@example.com");

            mockMvc.perform(post(URL).header("Authorization", bearer(account)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().maxAge("lunisoft_access_token", 0))
                    .andExpect(cookie().maxAge("lunisoft_refresh_token", 0));
        }
    }
}
