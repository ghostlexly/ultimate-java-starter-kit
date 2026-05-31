package com.lunisoft.javastarter.module.customer.controller.customercontroller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/** GET /api/customer/profile */
class GetProfileIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/customer/profile";

  @Test
  void returns_401_when_no_token() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  @Test
  void returns_200_with_customer_data_when_authenticated_as_customer() throws Exception {
    var account = fixtures.givenCustomer("customer-profile@example.com");
    var customer = account.getCustomer();

    mockMvc
        .perform(get(URL).header("Authorization", bearer(account)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customer.getId().toString()))
        .andExpect(jsonPath("$.email").value(account.getEmail()));
  }

  @Test
  void returns_403_when_authenticated_as_admin() throws Exception {
    var account = fixtures.givenAdmin("admin@example.com");

    // Admin role doesn't satisfy hasRole('CUSTOMER').
    mockMvc
        .perform(get(URL).header("Authorization", bearer(account)))
        .andExpect(status().isForbidden());
  }
}
