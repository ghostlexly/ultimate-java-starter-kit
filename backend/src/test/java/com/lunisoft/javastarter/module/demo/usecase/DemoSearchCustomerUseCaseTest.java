package com.lunisoft.javastarter.module.demo.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoSearchCustomerUseCaseTest {

  @Mock private DemoCustomerRepository demoCustomerRepository;

  @InjectMocks private DemoSearchCustomerUseCase demoSearchCustomerUseCase;

  @Test
  void execute_with_results_returns_mapped_responses() {
    var account1 = createCustomerAccount();
    account1.setEmail("alice@example.com");
    var customer1 = account1.getCustomer();

    var account2 = createCustomerAccount();
    account2.setEmail("bob@example.com");
    var customer2 = account2.getCustomer();

    when(demoCustomerRepository.findByAccountRole(Role.CUSTOMER))
        .thenReturn(List.of(customer1, customer2));

    var output = demoSearchCustomerUseCase.execute(Role.CUSTOMER);

    assertThat(output).hasSize(2);
    assertThat(output.get(0).email()).isEqualTo("alice@example.com");
    assertThat(output.get(0).role()).isEqualTo("CUSTOMER");
    assertThat(output.get(1).email()).isEqualTo("bob@example.com");
  }

  @Test
  void execute_no_results_returns_empty_list() {
    when(demoCustomerRepository.findByAccountRole(Role.ADMIN)).thenReturn(List.of());

    var output = demoSearchCustomerUseCase.execute(Role.ADMIN);

    assertThat(output).isEmpty();
  }
}
