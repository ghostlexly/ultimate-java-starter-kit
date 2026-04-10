package com.lunisoft.javastarter.module.demo.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DemoPaginateCustomerUseCaseTest {

  @Mock private DemoCustomerRepository demoCustomerRepository;

  private DemoPaginateCustomerUseCase demoPaginateCustomerUseCase;

  @BeforeEach
  void setUp() {
    demoPaginateCustomerUseCase = new DemoPaginateCustomerUseCase(demoCustomerRepository);
  }

  @Test
  void execute_returnsPagedResults() {
    var customer = createCustomer("test@example.com", Role.CUSTOMER);
    var pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    var page = new PageImpl<>(List.of(customer), pageable, 1);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var result = demoPaginateCustomerUseCase.execute(0, 10, null);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().email()).isEqualTo("test@example.com");
    assertThat(result.content().getFirst().role()).isEqualTo("CUSTOMER");
    assertThat(result.totalItems()).isEqualTo(1);
    assertThat(result.totalPages()).isEqualTo(1);
    assertThat(result.isFirst()).isTrue();
    assertThat(result.isLast()).isTrue();
  }

  @Test
  void execute_emptyResults_returnsEmptyPage() {
    var pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
    var page = new PageImpl<Customer>(List.of(), pageable, 0);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var result = demoPaginateCustomerUseCase.execute(0, 10, null);

    assertThat(result.content()).isEmpty();
    assertThat(result.totalItems()).isZero();
    assertThat(result.totalPages()).isZero();
    assertThat(result.isFirst()).isTrue();
    assertThat(result.isLast()).isTrue();
  }

  @Test
  void execute_withFilters_passesSpecificationToRepository() {
    var pageable = PageRequest.of(0, 5, Sort.by("id").ascending());
    var page = new PageImpl<Customer>(List.of(), pageable, 0);

    when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    var result = demoPaginateCustomerUseCase.execute(0, 5, "test@example.com");

    assertThat(result.content()).isEmpty();
  }
}
