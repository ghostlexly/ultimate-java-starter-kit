package com.lunisoft.javastarter.module.auth.controller.authcontroller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** POST /api/auth/logout */
class LogoutIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/logout";

  @Test
  void clears_auth_cookies() throws Exception {
    var account = fixtures.givenCustomer("logout@example.com");

    mockMvc
        .perform(post(URL).header("Authorization", bearer(account)))
        .andExpect(status().isOk())
        .andExpect(cookie().maxAge("lunisoft_access_token", 0))
        .andExpect(cookie().maxAge("lunisoft_refresh_token", 0));
  }
}
