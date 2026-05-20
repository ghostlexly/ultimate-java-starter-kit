package com.lunisoft.javastarter.module.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** POST /api/auth/refresh */
class RefreshTokensIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/refresh";

  @Test
  void returnsNewTokens_givenValidRefreshToken() throws Exception {
    var account = fixtures.givenCustomer("refresh@example.com");
    var session = fixtures.givenSession(account);
    var refreshToken = jwtTokenProvider.generateRefreshToken(session.getId());

    var body = jsonMapper.writeValueAsString(Map.of("refreshToken", refreshToken));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.role").value("CUSTOMER"));
  }

  @Test
  void returns400_whenTokenMissing() throws Exception {
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_TOKEN"));
  }

  @Test
  void returns401_whenTokenIsGarbage() throws Exception {
    var body = jsonMapper.writeValueAsString(Map.of("refreshToken", "not-a-real-jwt"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
  }
}
