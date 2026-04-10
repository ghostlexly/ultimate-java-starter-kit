package com.lunisoft.javastarter.module.demo.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
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
    var customer1 = createCustomer("alice@example.com", Role.CUSTOMER);
    var customer2 = createCustomer("bob@example.com", Role.CUSTOMER);

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
