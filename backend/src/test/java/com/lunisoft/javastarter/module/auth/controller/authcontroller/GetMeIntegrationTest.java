package com.lunisoft.javastarter.module.auth.controller.authcontroller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** GET /api/auth/me */
class GetMeIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/me";

  @Test
  void returns_401_when_unauthenticated() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void returns_authenticated_user_when_jwt_is_valid() throws Exception {
    var account = fixtures.givenCustomer("me@example.com");

    mockMvc
        .perform(get(URL).header("Authorization", bearer(account)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value(account.getId().toString()))
        .andExpect(jsonPath("$.email").value(account.getEmail()))
        .andExpect(jsonPath("$.role").value("CUSTOMER"));
  }
}
