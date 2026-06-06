package com.lunisoft.javastarter.module.demo.repository;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable filter specifications for Customer queries. Each method returns a Specification that can
 * be chained with .and() / .or().
 *
 * <p>Usage example:
 *
 * <pre>
 *   Specification<Customer> specs = Specification.unrestricted()
 *       .and(DemoCustomerSpecification.emailContaining("john"));
 *   repository.findAll(specs, pageable);
 * </pre>
 */
public final class DemoCustomerSpecification {

  private DemoCustomerSpecification() {}

  /** Filters customers whose account email contains the given value (case-insensitive). */
  public static Specification<Customer> emailContaining(String email) {
    return (root, _, cb) -> {
      var account = root.join("account", JoinType.LEFT);

      return cb.like(cb.lower(account.get("email")), "%" + email.trim().toLowerCase() + "%");
    };
  }
}
