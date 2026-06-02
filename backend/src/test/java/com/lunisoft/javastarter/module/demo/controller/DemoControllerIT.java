package com.lunisoft.javastarter.module.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Integration tests for {@link DemoController}. */
class DemoControllerIT extends AbstractIntegrationTest {

  /** GET /api/demo/customers */
  @Nested
  class SearchCustomers {

    private static final String URL = "/api/demo/customers";

    @Test
    void returns_searched_customers_list() throws Exception {
      var account = fixtures.givenCustomer("me@example.com");

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("role", "CUSTOMER");

      mockMvc
          .perform(get(URL).header("Authorization", bearer(account)).params(params))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].id").value(account.getCustomer().getId().toString()))
          .andExpect(jsonPath("$[0].email").value(account.getEmail()))
          .andExpect(jsonPath("$[0].role").value(Role.CUSTOMER.name()));
    }
  }
}
