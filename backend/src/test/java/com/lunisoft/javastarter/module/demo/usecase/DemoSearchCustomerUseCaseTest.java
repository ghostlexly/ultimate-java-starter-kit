package com.lunisoft.javastarter.module.demo.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.usecase.searchcustomer.DemoSearchCustomerUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DemoSearchCustomerUseCaseTest {

  @Mock private DemoCustomerRepository demoCustomerRepository;

  @InjectMocks
  private DemoSearchCustomerUseCase demoSearchCustomerUseCase;


  @Test
  void execute_withResults_returnsMappedResponses() {
    var account1 = createCustomerAccount();
    account1.setEmail("alice@example.com");
    var customer1 = account1.getCustomer();

    var account2 = createCustomerAccount();
    account2.setEmail("bob@example.com");
    var customer2 = account2.getCustomer();

    when(demoCustomerRepository.findByAccountRole(Role.CUSTOMER))
        .thenReturn(List.of(customer1, customer2));

    var result = demoSearchCustomerUseCase.execute(Role.CUSTOMER);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).email()).isEqualTo("alice@example.com");
    assertThat(result.get(0).role()).isEqualTo("CUSTOMER");
    assertThat(result.get(1).email()).isEqualTo("bob@example.com");
  }

  @Test
  void execute_noResults_returnsEmptyList() {
    when(demoCustomerRepository.findByAccountRole(Role.ADMIN)).thenReturn(List.of());

    var result = demoSearchCustomerUseCase.execute(Role.ADMIN);

    assertThat(result).isEmpty();
  }
}
