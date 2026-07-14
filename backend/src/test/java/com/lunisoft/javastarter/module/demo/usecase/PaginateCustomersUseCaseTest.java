package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.pagination.PaginationService;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    @Mock
    private DemoCustomerRepository demoCustomerRepository;

    // Real instance: PaginationService is pure logic with no dependencies, so we
    // exercise the actual page/sort resolution instead of stubbing it.
    @Spy
    private PaginationService paginationService;

    @InjectMocks
    private PaginateCustomersUseCase paginateCustomersUseCase;

    @Test
    void execute_returns_paged_results() {
        var account = createCustomerAccount();
        var customer = account.getCustomer();
        var pageable = PageRequest.of(0, 10, DEFAULT_SORT);
        var page = new PageImpl<>(List.of(customer), pageable, 1);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(1, 10, null, null, null);
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
        var pageable = PageRequest.of(0, 10, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(1, 10, null, null, null);
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
        assertThat(output.totalItems()).isZero();
        assertThat(output.totalPages()).isZero();
        assertThat(output.isFirst()).isTrue();
        assertThat(output.isLast()).isTrue();
    }

    @Test
    void execute_with_filters_passes_specification_to_repository() {
        var pageable = PageRequest.of(0, 5, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(1, 5, null, null, "test@example.com");
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
    }

    @Test
    void execute_with_whitelisted_sort_resolves_entity_properties() {
        // The "name" sort key maps to lastName + firstName in the whitelist.
        var expectedSort = Sort.by(Sort.Direction.DESC, "lastName", "firstName");
        var pageable = PageRequest.of(0, 10, expectedSort);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(1, 10, "name", "desc", null);
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
    }

    @Test
    void execute_with_unknown_sort_falls_back_to_default_sort() {
        var pageable = PageRequest.of(0, 10, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var input = new PaginateCustomersUseCase.Input(1, 10, "notAWhitelistedKey", "asc", null);
        var output = paginateCustomersUseCase.execute(input);

        assertThat(output.content()).isEmpty();
    }
}
