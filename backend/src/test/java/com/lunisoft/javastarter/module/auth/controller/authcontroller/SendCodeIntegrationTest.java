package com.lunisoft.javastarter.module.auth.controller.authcontroller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import com.lunisoft.javastarter.shared.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** POST /api/auth/send-code */
class SendCodeIntegrationTest extends AbstractIntegrationTest {

  private static final String URL = "/api/auth/send-code";

  @Autowired private AccountRepository accountRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private VerificationTokenRepository verificationTokenRepository;

  @Test
  void creates_account_and_verification_token_for_new_email() throws Exception {
    var email = "new-user@example.com";
    var body = jsonMapper.writeValueAsString(Map.of("email", email));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Login code sent successfully."));

    var account = accountRepository.findByEmail(email).orElseThrow();
    assertThat(account.getRole()).isEqualTo(Role.CUSTOMER);
    assertThat(customerRepository.findByAccountId(account.getId())).isPresent();

    var token =
        verificationTokenRepository
            .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
                account.getId(), VerificationType.LOGIN_CODE)
            .orElseThrow();
    assertThat(token.getValue()).hasSize(4).containsOnlyDigits();
  }

  @Test
  void returns_400_when_email_is_invalid() throws Exception {
    var body = jsonMapper.writeValueAsString(Map.of("email", "not-an-email"));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void returns_429_when_requested_within_cooldown() throws Exception {
    var account = fixtures.givenCustomer("cooldown-user@example.com");
    fixtures.givenLoginCode(account, "1234");

    var body = jsonMapper.writeValueAsString(Map.of("email", account.getEmail()));

    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("LOGIN_CODE_COOLDOWN"));
  }
}
