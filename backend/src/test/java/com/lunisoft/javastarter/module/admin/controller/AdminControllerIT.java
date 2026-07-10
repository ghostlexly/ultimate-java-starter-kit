package com.lunisoft.javastarter.module.admin.controller;

import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for {@link AdminController}. */
class AdminControllerIT extends AbstractIntegrationTest {

    /** GET /api/admin/stats */
    @Nested
    class GetStats {

        private static final String URL = "/api/admin/stats";

        @Test
        void returns_401_when_no_token() throws Exception {
            mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        }

        @Test
        void returns_403_when_authenticated_as_customer() throws Exception {
            var account = fixtures.givenCustomer("not-admin@example.com");

            mockMvc.perform(get(URL).header("Authorization", bearer(account))).andExpect(status().isForbidden());
        }

        @Test
        void returns_200_with_counts_when_authenticated_as_admin() throws Exception {
            var admin = fixtures.givenAdmin("admin-stats@example.com");
            fixtures.givenCustomer("c1@example.com");
            fixtures.givenCustomer("c2@example.com");

            mockMvc.perform(get(URL).header("Authorization", bearer(admin)))
                    .andExpect(status().isOk())
                    // 2 customers + 1 admin
                    .andExpect(jsonPath("$.accounts").value(3))
                    .andExpect(jsonPath("$.activeSessions").value(0));
        }
    }
}
