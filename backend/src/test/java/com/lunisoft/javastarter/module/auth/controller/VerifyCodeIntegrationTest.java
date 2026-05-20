package com.lunisoft.javastarter.module.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** POST /api/auth/verify-code */
class VerifyCodeIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/verify-code";

  @Autowired private VerificationTokenRepository verificationTokenRepository;
  @Autowired private SessionRepository sessionRepository;

  @Test
  void returnsTokensAndSetsCookies_onValidCode() throws Exception {
    var account = fixtures.givenCustomer("verify-success@example.com");
    fixtures.givenLoginCode(account, "4321");

    var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "4321"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("CUSTOMER"))
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(cookie().exists("lunisoft_access_token"))
        .andExpect(cookie().exists("lunisoft_refresh_token"))
        .andExpect(cookie().httpOnly("lunisoft_access_token", true));

    // Code is consumed exactly once.
    assertThat(
            verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
                account.getId(), VerificationType.LOGIN_CODE))
        .isEmpty();
    // A real session row was persisted.
    assertThat(sessionRepository.count()).isEqualTo(1);
  }

  @Test
  void returns400_onWrongCode() throws Exception {
    var account = fixtures.givenCustomer("wrong-code@example.com");
    fixtures.givenLoginCode(account, "1111");

    var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "9999"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_CODE"));

    // Wrong attempts must still be counted even though the transaction "fails".
    var token =
        verificationTokenRepository
            .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
                account.getId(), VerificationType.LOGIN_CODE)
            .orElseThrow();
    assertThat(token.getAttempts()).isEqualTo(1);
  }

  @Test
  void returns429_whenMaxAttemptsReached() throws Exception {
    var account = fixtures.givenCustomer("max-attempts@example.com");
    fixtures.givenLoginCode(account, "1111", t -> t.setAttempts(5));

    var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "1111"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("MAX_ATTEMPTS_REACHED"));
  }

  @Test
  void returns400_whenCodeExpired() throws Exception {
    var account = fixtures.givenCustomer("expired-code@example.com");
    fixtures.givenLoginCode(
        account, "2222", t -> t.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES)));

    var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail(), "code", "2222"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_CODE"));
  }
}
