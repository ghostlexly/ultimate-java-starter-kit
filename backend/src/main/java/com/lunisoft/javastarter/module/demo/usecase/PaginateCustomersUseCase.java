package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.dto.PaginatedResponse;
import com.lunisoft.javastarter.core.pagination.SortWhitelist;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerSpecification;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
@NullMarked
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

    public record Output(UUID id, String email, String role) {}

    @Transactional(readOnly = true)
    public PaginatedResponse<Output> execute(Pageable pageable, @Nullable String email) {
        Pageable sortedPageable = SortWhitelist.apply(pageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        Specification<Customer> specs = this.buildSpecs(email);

        Page<Output> page =
                demoCustomerRepository.findAll(specs, sortedPageable).map(this::toOutput);

        return PaginatedResponse.from(page);
    }

    /**
     * Builds the specification by chaining optional filters onto the base spec.
     */
    private Specification<Customer> buildSpecs(@Nullable String email) {
        List<Specification<Customer>> specs = new ArrayList<>();

        if (StringUtils.hasText(email)) {
            specs.add(DemoCustomerSpecification.emailContaining(email));
        }

        return Specification.allOf(specs);
    }

    private Output toOutput(Customer customer) {
        return new Output(
                customer.getId(),
                customer.getAccount().getEmail(),
                customer.getAccount().getRole().name());
    }
}
