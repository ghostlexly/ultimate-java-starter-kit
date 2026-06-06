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
import org.springframework.transaction.annotation.Transactional;

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
  public record Output(
      List<CustomerItem> content,
      long totalItems,
      int totalPages,
      boolean isFirst,
      boolean isLast) {

    public record CustomerItem(UUID id, String email, String role) {}
  }

  @Transactional(readOnly = true)
  public Output execute(Input input) {
    Pageable pageable =
        PageRequest.of(input.page(), input.size(), Sort.by("createdAt").ascending());
    Specification<Customer> specs = buildSpecs(input.email);

    var page = demoCustomerRepository.findAll(specs, pageable);

    var content =
        page.getContent().stream()
            .map(
                customer ->
                    new Output.CustomerItem(
                        customer.getId(),
                        customer.getAccount() != null ? customer.getAccount().getEmail() : null,
                        customer.getAccount() != null
                            ? customer.getAccount().getRole().name()
                            : null))
            .toList();

    return new Output(
        content, page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
  }

  /** Builds the specification by chaining optional filters onto the base spec. */
  private Specification<Customer> buildSpecs(String email) {
    Specification<Customer> specs = Specification.unrestricted();

    if (email != null) {
      specs = specs.and(DemoCustomerSpecification.emailContaining(email));
    }

    return specs;
  }
}
