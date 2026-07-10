package com.lunisoft.javastarter.module.customer.controller;

import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integration tests for {@link CustomerController}. */
class CustomerControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    /** GET /api/customer/profile */
    @Nested
    class GetProfile {

        private static final String URL = "/api/customer/profile";

        @Test
        void returns_401_when_no_token() throws Exception {
            mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        }

        @Test
        void returns_200_with_customer_data_when_authenticated_as_customer() throws Exception {
            var account = fixtures.givenCustomer("customer-profile@example.com");
            var customer = account.getCustomer();

            mockMvc.perform(get(URL).header("Authorization", bearer(account)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.email").value(account.getEmail()));
        }

        @Test
        void returns_403_when_authenticated_as_admin() throws Exception {
            var account = fixtures.givenAdmin("admin@example.com");

            // Admin role doesn't satisfy hasRole('CUSTOMER').
            mockMvc.perform(get(URL).header("Authorization", bearer(account))).andExpect(status().isForbidden());
        }
    }

    /** PATCH /api/customer/email */
    @Nested
    class UpdateEmail {

        private static final String URL = "/api/customer/email";

        @Test
        void returns_200_and_persists_new_email_when_authenticated_as_customer() throws Exception {
            var account = fixtures.givenCustomer("update-email@example.com");
            var customer = account.getCustomer();

            var body = jsonMapper.writeValueAsString(Map.of("email", "changed@example.com"));

            mockMvc.perform(patch(URL)
                            .header("Authorization", bearer(account))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                    .andExpect(jsonPath("$.email").value("changed@example.com"));

            // The new email is actually committed to the database.
            var reloaded = accountRepository.findById(account.getId()).orElseThrow();
            assertThat(reloaded.getEmail()).isEqualTo("changed@example.com");
        }

        @Test
        void returns_400_when_email_is_invalid() throws Exception {
            var account = fixtures.givenCustomer("invalid-email@example.com");

            var body = jsonMapper.writeValueAsString(Map.of("email", "not-an-email"));

            mockMvc.perform(patch(URL)
                            .header("Authorization", bearer(account))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            // The invalid request leaves the stored email untouched.
            var reloaded = accountRepository.findById(account.getId()).orElseThrow();
            assertThat(reloaded.getEmail()).isEqualTo("invalid-email@example.com");
        }

        @Test
        void returns_401_when_no_token() throws Exception {
            var body = jsonMapper.writeValueAsString(Map.of("email", "changed@example.com"));

            mockMvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void returns_403_when_authenticated_as_admin() throws Exception {
            var account = fixtures.givenAdmin("admin-update-email@example.com");

            var body = jsonMapper.writeValueAsString(Map.of("email", "changed@example.com"));

            // Admin role doesn't satisfy hasRole('CUSTOMER').
            mockMvc.perform(patch(URL)
                            .header("Authorization", bearer(account))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }
}
