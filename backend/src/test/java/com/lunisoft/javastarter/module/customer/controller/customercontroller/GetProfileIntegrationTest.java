package com.lunisoft.javastarter.module.customer.controller.customercontroller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** GET /api/customer/profile */
class GetProfileIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/customer/profile";

  @Test
  void returns401_whenNoToken() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void returns200_withCustomerData_whenAuthenticatedAsCustomer() throws Exception {
    var account = fixtures.givenCustomer("customer-profile@example.com");
    var customer = account.getCustomer();

    var token =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), account.getId(), account.getEmail(), Role.CUSTOMER);

    mockMvc
        .perform(get(URL).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
        .andExpect(jsonPath("$.email").value(account.getEmail()));
  }

  @Test
  void returns403_whenAuthenticatedAsAdmin() throws Exception {
    var account = fixtures.givenAdmin("admin@example.com");
    var token =
        jwtTokenProvider.generateAccessToken(
            UUID.randomUUID(), account.getId(), account.getEmail(), Role.ADMIN);

    // Admin role doesn't satisfy hasRole('CUSTOMER').
    mockMvc
        .perform(get(URL).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
