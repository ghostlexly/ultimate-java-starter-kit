package com.lunisoft.ultimatejavastarterkit.module.customer.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GetProfileUseCaseTest {

  @Mock private CustomerRepository customerRepository;

  private GetProfileUseCase getProfileUseCase;

  @BeforeEach
  void setUp() {
    getProfileUseCase = new GetProfileUseCase(customerRepository);
  }

  @Test
  void execute_existingProfile_returnsCustomerResponse() {
    var accountId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var account = new Account();
    account.setId(accountId);
    account.setEmail("test@example.com");
    account.setRole(Role.CUSTOMER);

    var customer = new Customer();
    customer.setId(customerId);
    customer.setAccount(account);
    customer.setCountryCode("FR");

    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));

    var result = getProfileUseCase.execute(accountId);

    assertThat(result.id()).isEqualTo(customerId);
    assertThat(result.email()).isEqualTo("test@example.com");
    assertThat(result.countryCode()).isEqualTo("FR");
  }

  @Test
  void execute_profileNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getProfileUseCase.execute(accountId))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }
}
