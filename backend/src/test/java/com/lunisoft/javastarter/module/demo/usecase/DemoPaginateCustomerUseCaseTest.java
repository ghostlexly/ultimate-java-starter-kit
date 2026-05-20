package com.lunisoft.javastarter.module.demo.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DemoPaginateCustomerUseCaseTest {

  @Mock private DemoCustomerRepository demoCustomerRepository;

  @InjectMocks private DemoPaginateCustomerUseCase demoPaginateCustomerUseCase;

  @Test
  void execute_returns_paged_results() {
    var account = createCustomerAccount();
    var customer = account.getCustomer();
    var pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
    var page = new PageImpl<>(List.of(customer), pageable, 1);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var input = new DemoPaginateCustomerUseCase.Input(0, 10, null);
    var output = demoPaginateCustomerUseCase.execute(input);

    assertThat(output.content()).hasSize(1);
    assertThat(output.content().getFirst().email()).isEqualTo("contact+customer@lunisoft.fr");
    assertThat(output.content().getFirst().role()).isEqualTo("CUSTOMER");
    assertThat(output.totalItems()).isEqualTo(1);
    assertThat(output.totalPages()).isEqualTo(1);
    assertThat(output.isFirst()).isTrue();
    assertThat(output.isLast()).isTrue();
  }

  @Test
  void execute_empty_results_returns_empty_page() {
    var pageable = PageRequest.of(0, 10, Sort.by("createdAt").ascending());
    var page = new PageImpl<Customer>(List.of(), pageable, 0);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var input = new DemoPaginateCustomerUseCase.Input(0, 10, null);
    var output = demoPaginateCustomerUseCase.execute(input);

    assertThat(output.content()).isEmpty();
    assertThat(output.totalItems()).isZero();
    assertThat(output.totalPages()).isZero();
    assertThat(output.isFirst()).isTrue();
    assertThat(output.isLast()).isTrue();
  }

  @Test
  void execute_with_filters_passes_specification_to_repository() {
    var pageable = PageRequest.of(0, 5, Sort.by("createdAt").ascending());
    var page = new PageImpl<Customer>(List.of(), pageable, 0);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var input = new DemoPaginateCustomerUseCase.Input(0, 5, "test@example.com");
    var output = demoPaginateCustomerUseCase.execute(input);

    assertThat(output.content()).isEmpty();
  }
}
