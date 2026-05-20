package com.lunisoft.javastarter.module.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** GET /api/auth/me */
class GetMeIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/me";

  @Test
  void returns401_whenUnauthenticated() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void returnsAuthenticatedUser_whenJwtIsValid() throws Exception {
    var account = fixtures.givenCustomer("me@example.com");
    var accessToken =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), account.getId(), account.getEmail(), Role.CUSTOMER);

    mockMvc
        .perform(get(URL).header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
        .andExpect(jsonPath("$.email").value(account.getEmail()))
        .andExpect(jsonPath("$.role").value("CUSTOMER"));
  }
}
