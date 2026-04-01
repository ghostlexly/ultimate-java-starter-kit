package com.lunisoft.ultimatejavastarterkit.module.demo.usecase;

import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.demo.dto.DemoPaginatedCustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.demo.repository.DemoCustomerSpecification;
import com.lunisoft.ultimatejavastarterkit.module.demo.repository.DemoCustomerRepository;
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
public class DemoPaginateCustomerUseCase {

  private final DemoCustomerRepository demoCustomerRepository;

  public DemoPaginateCustomerUseCase(DemoCustomerRepository demoCustomerRepository) {
    this.demoCustomerRepository = demoCustomerRepository;
  }

  public DemoPaginatedCustomerResponse execute(int page, int size, String email, String countryCode) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
    Specification<Customer> spec = buildSpec(email, countryCode);

    var result = demoCustomerRepository.findAll(spec, pageable);

    var items =
        result.getContent().stream()
            .map(
                customer ->
                    new DemoPaginatedCustomerResponse.CustomerItem(
                        customer.getId(),
                        customer.getAccount().getEmail(),
                        customer.getCountryCode(),
                        customer.getAccount().getRole().name()))
            .toList();

    return new DemoPaginatedCustomerResponse(
        items,
        result.getTotalElements(),
        result.getTotalPages(),
        result.isFirst(),
        result.isLast());
  }

  /** Builds the specification by chaining optional filters onto the base fetchAccount() spec. */
  private Specification<Customer> buildSpec(String email, String countryCode) {
    Specification<Customer> spec = DemoCustomerSpecification.fetchAccount();

    if (email != null && !email.isBlank()) {
      spec = spec.and(DemoCustomerSpecification.hasEmail(email.trim()));
    }

    if (countryCode != null && !countryCode.isBlank()) {
      spec = spec.and(DemoCustomerSpecification.hasCountryCode(countryCode.trim()));
    }

    return spec;
  }
}
