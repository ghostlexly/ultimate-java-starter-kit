package com.lunisoft.ultimatejavastarterkit.module.demo.repository;

import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable filter specifications for Customer queries. Each method returns a Specification that can
 * be chained with .and() / .or().
 *
 * <p>Usage example:
 *
 * <pre>
 *   Specification<Customer> spec = DemoCustomerSpecification.fetchAccount()
 *       .and(DemoCustomerSpecification.hasEmail("john"))
 *       .and(DemoCustomerSpecification.hasCountryCode("FR"));
 *   repository.findAll(spec, pageable);
 * </pre>
 */
public final class DemoCustomerSpecification {

  private DemoCustomerSpecification() {}

  /**
   * Eagerly fetches the account relation to avoid LazyInitializationException. Should always be
   * used as the base spec when mapping results that access account fields.
   */
  public static Specification<Customer> build() {
    return (root, query, cb) -> {
      // Only fetch for the main query, not for the count query (which doesn't support fetch)
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("account");
      }

      return cb.conjunction();
    };
  }

  /** Filters customers whose account email contains the given value (case-insensitive). */
  public static Specification<Customer> hasEmail(String email) {
    return (root, query, cb) ->
        cb.like(cb.lower(root.join("account").get("email")), "%" + email.toLowerCase() + "%");
  }

  /** Filters customers by exact country code match. */
  public static Specification<Customer> hasCountryCode(String countryCode) {
    return (root, query, cb) -> cb.equal(root.get("countryCode"), countryCode);
  }
}
