package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.dto.PaginatedResponse;
import com.lunisoft.javastarter.core.pagination.SortWhitelist;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo use case: paginated search of customers with optional filters. Illustrates how to use
 * JpaSpecificationExecutor for dynamic filtering.
 *
 * <p>
 * Paginated response containing a list of customers and pagination metadata. Example: GET
 * /api/demo/customers/paginated?page=1&size=10&email=john
 * </p>
 *
 * <p>
 * Each non-null filter adds a WHERE clause via Specification. Filters are composable: adding a new
 * one is just another .and() call.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PaginateCustomersUseCase {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    // Whitelist of API sort keys mapped to entity property paths — anything else falls
    // back to the default sort, so clients can never sort on arbitrary columns.
    private static final Map<String, List<String>> SORTABLE_PROPERTIES = Map.of(
            "createdAt", List.of("createdAt"),
            "name", List.of("lastName", "firstName"),
            "email", List.of("account.email"),
            "role", List.of("account.role"),
            "isActive", List.of("isActive"));

    private final DemoCustomerRepository demoCustomerRepository;

    public record PaginateCustomersQuery(Pageable pageable, String email) {}

    public record PaginateCustomersResult(UUID id, String email, String role) {}

    @Transactional(readOnly = true)
    public PaginatedResponse<PaginateCustomersResult> execute(PaginateCustomersQuery query) {
        Assert.notNull(query, "Query cannot be null");
        Assert.notNull(query.pageable(), "Pageable cannot be null");

        Pageable pageable = SortWhitelist.apply(query.pageable(), SORTABLE_PROPERTIES, DEFAULT_SORT);

        Specification<Customer> specs = buildSpecs(query);

        Page<PaginateCustomersResult> page = demoCustomerRepository.findAll(specs, pageable).map(this::toResult);

        return PaginatedResponse.from(page);
    }

    /**
     * Builds the specification by chaining optional filters onto the base spec.
     */
    private Specification<Customer> buildSpecs(PaginateCustomersQuery query) {
        List<Specification<Customer>> specs = new ArrayList<>();

        if (StringUtils.hasText(query.email())) {
            specs.add(DemoCustomerSpecification.emailContaining(query.email()));
        }

        return Specification.allOf(specs);
    }

    private PaginateCustomersResult toResult(Customer customer) {
        return new PaginateCustomersResult(
                customer.getId(),
                customer.getAccount() != null ? customer.getAccount().getEmail() : null,
                customer.getAccount() != null ? customer.getAccount().getRole().name() : null);
    }
}
