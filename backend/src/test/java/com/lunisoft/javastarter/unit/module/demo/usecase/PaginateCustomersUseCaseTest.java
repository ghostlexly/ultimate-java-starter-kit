package com.lunisoft.javastarter.unit.module.demo.usecase;

import com.lunisoft.javastarter.module.demo.usecase.PaginateCustomersUseCase ;
import com.lunisoft.javastarter.module.demo.usecase.PaginateCustomersUseCase.PaginateCustomersQuery;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static com.lunisoft.javastarter.unit.support.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaginateCustomersUseCaseTest {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    @Mock
    private DemoCustomerRepository demoCustomerRepository;

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

        var query = new PaginateCustomersQuery(PageRequest.of(0, 10), null);
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().email()).isEqualTo("contact+customer@lunisoft.fr");
        assertThat(result.content().getFirst().role()).isEqualTo("CUSTOMER");
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void execute_empty_results_returns_empty_page() {
        var pageable = PageRequest.of(0, 10, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var query = new PaginateCustomersQuery(PageRequest.of(0, 10), null);
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalItems()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void execute_with_filters_passes_specification_to_repository() {
        var pageable = PageRequest.of(0, 5, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var query = new PaginateCustomersQuery(PageRequest.of(0, 5), "test@example.com");
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void execute_keeps_requested_page_and_size() {
        // Third page of 5 items: the requested page/size reach the repository untouched,
        // only the sort is resolved (here: unsorted -> default sort).
        var pageable = PageRequest.of(2, 5, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 11);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var query = new PaginateCustomersQuery(PageRequest.of(2, 5), null);
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.totalItems()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.isFirst()).isFalse();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void execute_with_whitelisted_sort_resolves_entity_properties() {
        // The "name" sort key maps to lastName + firstName in the whitelist.
        var expectedSort = Sort.by(Sort.Direction.DESC, "lastName", "firstName");
        var pageable = PageRequest.of(0, 10, expectedSort);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var requestedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));
        var query = new PaginateCustomersQuery(requestedPageable, null);
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void execute_with_unknown_sort_falls_back_to_default_sort() {
        var pageable = PageRequest.of(0, 10, DEFAULT_SORT);
        var page = new PageImpl<Customer>(List.of(), pageable, 0);

        when(demoCustomerRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(page);

        var requestedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "notAWhitelistedKey"));
        var query = new PaginateCustomersQuery(requestedPageable, null);
        var result = paginateCustomersUseCase.execute(query);

        assertThat(result.content()).isEmpty();
    }
}
