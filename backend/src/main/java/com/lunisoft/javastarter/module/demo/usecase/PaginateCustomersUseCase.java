package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.dto.PageQuery;
import com.lunisoft.javastarter.core.dto.PaginatedResponse;
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

import java.util.ArrayList;
import java.util.List;
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

    private final DemoCustomerRepository demoCustomerRepository;

    public record Input(int page, int size, String email) {}

    public record Output(UUID id, String email, String role) {}

    @Transactional(readOnly = true)
    public PaginatedResponse<Output> execute(Input input) {
        Assert.notNull(input, "Input cannot be null");
        Assert.isTrue(input.page() >= 0, "Page cannot be negative");
        Assert.isTrue(input.size() > 0, "Size cannot be zero or negative");

        Pageable pageable = new PageQuery(input.page(), input.size())
                .toPageable(Sort.by("createdAt").ascending());

        Specification<Customer> specs = buildSpec(input.email());

        Page<Output> page = demoCustomerRepository.findAll(specs, pageable).map(this::toOutput);

        return PaginatedResponse.from(page);
    }

    /**
     * Builds the specification by chaining optional filters onto the base spec.
     */
    private Specification<Customer> buildSpec(String email) {
        List<Specification<Customer>> specs = new ArrayList<>();

        if (email != null) {
            specs.add(DemoCustomerSpecification.emailContaining(email));
        }

        return Specification.allOf(specs);
    }

    private Output toOutput(Customer customer) {
        return new Output(
                customer.getId(),
                customer.getAccount() != null ? customer.getAccount().getEmail() : null,
                customer.getAccount() != null ? customer.getAccount().getRole().name() : null);
    }
}
