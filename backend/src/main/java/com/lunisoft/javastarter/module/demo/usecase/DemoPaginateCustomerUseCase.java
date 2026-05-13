package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerSpecification;
import java.util.List;
import java.util.UUID;
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

  public record Input(int page, int size, String email) {}

  /**
   * Paginated response containing a list of customers and pagination metadata. Example: GET
   * /api/demo/customers/paginated?page=1&size=10&email=john
   */
  public record Result(
      List<CustomerItem> content,
      long totalItems,
      int totalPages,
      boolean isFirst,
      boolean isLast) {

    public record CustomerItem(UUID id, String email, String role) {}
  }

  public Result execute(Input input) {
    Pageable pageable =
        PageRequest.of(input.page(), input.size(), Sort.by("createdAt").ascending());
    Specification<Customer> spec = buildSpec(input.email());

    var result = demoCustomerRepository.findAll(spec, pageable);

    var items =
        result.getContent().stream()
            .map(
                customer ->
                    new Result.CustomerItem(
                        customer.getId(),
                        customer.getAccount().getEmail(),
                        customer.getAccount().getRole().name()))
            .toList();

    return new Result(
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
