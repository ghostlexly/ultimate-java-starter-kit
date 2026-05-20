package com.lunisoft.javastarter.module.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** GET /api/admin/stats */
class GetStatsIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/admin/stats";

  @Test
  void returns401_whenNoToken() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void returns403_whenAuthenticatedAsCustomer() throws Exception {
    var account = fixtures.givenCustomer("not-admin@example.com");
    var token =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), account.getId(), account.getEmail(), Role.CUSTOMER);

    mockMvc
        .perform(get(URL).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void returns200_withCounts_whenAuthenticatedAsAdmin() throws Exception {
    var admin = fixtures.givenAdmin("admin-stats@example.com");
    fixtures.givenCustomer("c1@example.com");
    fixtures.givenCustomer("c2@example.com");

    var token =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), admin.getId(), admin.getEmail(), Role.ADMIN);

    mockMvc
        .perform(get(URL).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        // 2 customers + 1 admin
        .andExpect(jsonPath("$.accounts").value(3))
        .andExpect(jsonPath("$.activeSessions").value(0));
  }
}
