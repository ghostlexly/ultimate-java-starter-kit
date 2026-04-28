package com.lunisoft.javastarter.module.demo.usecase.paginatecustomer;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Demo use case: paginated search of customers with optional filters. Illustrates how to use
 * JpaSpecificationExecutor for dynamic filtering.
 *
 * <p>Each non-null filter adds a WHERE clause via Specification. Filters are composable: adding a
 * new one is just another .and() call.
 */
@Service
@RequiredArgsConstructor
public class DemoPaginateCustomerUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  public DemoPaginateCustomerResult execute(DemoPaginateCustomerInput input) {
    Pageable pageable =
        PageRequest.of(input.page(), input.size(), Sort.by("createdAt").descending());
    Specification<Customer> spec = buildSpec(input.email());

    var result = demoCustomerRepository.findAll(spec, pageable);

    var items =
        result.getContent().stream()
            .map(
                customer ->
                    new DemoPaginateCustomerResult.CustomerItem(
                        customer.getId(),
                        customer.getAccount().getEmail(),
                        customer.getAccount().getRole().name()))
            .toList();

    return new DemoPaginateCustomerResult(
        items,
        result.getTotalElements(),
        result.getTotalPages(),
        result.isFirst(),
        result.isLast());
  }

  /** Builds the specification by chaining optional filters onto the base spec. */
  private Specification<Customer> buildSpec(String email) {
    Specification<Customer> spec = DemoCustomerSpecification.build();

    if (email != null && !email.isBlank()) {
      spec = spec.and(DemoCustomerSpecification.hasEmail(email.trim()));
    }

    return spec;
  }
}
