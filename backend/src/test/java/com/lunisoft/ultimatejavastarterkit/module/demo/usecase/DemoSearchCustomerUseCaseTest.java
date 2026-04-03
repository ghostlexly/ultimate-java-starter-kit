package com.lunisoft.ultimatejavastarterkit.module.demo.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoSearchCustomerUseCaseTest {

  @Mock private DemoCustomerRepository demoCustomerRepository;

  private DemoSearchCustomerUseCase demoSearchCustomerUseCase;

  @BeforeEach
  void setUp() {
    demoSearchCustomerUseCase = new DemoSearchCustomerUseCase(demoCustomerRepository);
  }

  @Test
  void execute_withResults_returnsMappedResponses() {
    var customer1 = createCustomer("alice@example.com", "FR", Role.CUSTOMER);
    var customer2 = createCustomer("bob@example.com", "FR", Role.CUSTOMER);

    when(demoCustomerRepository.findByCountryCodeAndAccountRole("FR", Role.CUSTOMER))
        .thenReturn(List.of(customer1, customer2));

    var result = demoSearchCustomerUseCase.execute("FR", Role.CUSTOMER);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).email()).isEqualTo("alice@example.com");
    assertThat(result.get(0).countryCode()).isEqualTo("FR");
    assertThat(result.get(0).role()).isEqualTo("CUSTOMER");
    assertThat(result.get(1).email()).isEqualTo("bob@example.com");
  }

  @Test
  void execute_noResults_returnsEmptyList() {
    when(demoCustomerRepository.findByCountryCodeAndAccountRole("XX", Role.ADMIN))
        .thenReturn(List.of());

    var result = demoSearchCustomerUseCase.execute("XX", Role.ADMIN);

    assertThat(result).isEmpty();
  }

  private Customer createCustomer(String email, String countryCode, Role role) {
    var account = new Account();
    account.setId(UUID.randomUUID());
    account.setEmail(email);
    account.setRole(role);

    var customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setAccount(account);
    customer.setCountryCode(countryCode);

    return customer;
  }
}
