package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.core.dto.PageQuery;
import com.lunisoft.javastarter.core.dto.PaginatedResponse;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerSpecification;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Demo use case: paginated search of customers with optional filters. Illustrates how to use
 * JpaSpecificationExecutor for dynamic filtering.
 *
 * <p>Each non-null filter adds a WHERE clause via Specification. Filters are composable: adding a
 * new one is just another .and() call.
 */
@Service
@RequiredArgsConstructor
public class PaginateCustomersUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  public record Input(int page, int size, String email) {

  }

  /**
   * Paginated response containing a list of customers and pagination metadata. Example: GET
   * /api/demo/customers/paginated?page=1&size=10&email=john
   */
  public record Output(
      UUID id, String email, String role
  ) {

    static Output from(Customer customer) {
      return new Output(
          customer.getId(),
          customer.getAccount() != null ? customer.getAccount().getEmail() : null,
          customer.getAccount() != null
              ? customer.getAccount().getRole().name()
              : null);
    }

  }

  @Transactional(readOnly = true)
  public PaginatedResponse<Output> execute(Input input) {
    Assert.notNull(input, "Input cannot be null");
    Assert.isTrue(input.page() >= 0, "Page cannot be negative");
    Assert.isTrue(input.size() > 0, "Size cannot be zero or negative");

    Pageable pageable = new PageQuery(input.page(), input.size()).toPageable(
        Sort.by("createdAt").ascending());
    Specification<Customer> specs = buildSpecs(input.email());

    Page<Output> page = demoCustomerRepository.findAll(specs, pageable).map(Output::from);

    return PaginatedResponse.from(page);
  }


  /**
   * Builds the specification by chaining optional filters onto the base spec.
   */
  private Specification<Customer> buildSpecs(String email) {
    Specification<Customer> specs = Specification.unrestricted();

    if (email != null) {
      specs = specs.and(DemoCustomerSpecification.emailContaining(email));
    }

    return specs;
  }
}
