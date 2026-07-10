package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.dto.PageQuery;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaginateCustomersUseCaseTest {

    @Mock
    private DemoCustomerRepository demoCustomerRepository;

    @InjectMocks
    private PaginateCustomersUseCase paginateCustomersUseCase;

    @Test
    void execute_returns_paged_results() {
        var account = createCustomerAccount();
        var customer = account.getCustomer();
        var pageable = new PageQuery(1, 10).toPageable(Sort.by("createdAt").ascending());
        var page = new PageImpl<>(List.of(customer), pageable, 1);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(0, 10, null);
        var output = paginateCustomersUseCase.execute(input);

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
        var pageable = new PageQuery(1, 10).toPageable(Sort.by("createdAt").ascending());
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(0, 10, null);
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
        assertThat(output.totalItems()).isZero();
        assertThat(output.totalPages()).isZero();
        assertThat(output.isFirst()).isTrue();
        assertThat(output.isLast()).isTrue();
    }

    @Test
    void execute_with_filters_passes_specification_to_repository() {
        var pageable = new PageQuery(1, 5).toPageable(Sort.by("createdAt").ascending());
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(0, 5, "test@example.com");
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
    }
}
