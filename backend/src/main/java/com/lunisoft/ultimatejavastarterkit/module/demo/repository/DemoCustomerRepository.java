package com.lunisoft.ultimatejavastarterkit.module.demo.repository;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemoCustomerRepository extends JpaRepository<Customer, UUID> {

  // Needs to be in a @Transactional to loads the account relation which is set as Lazy
  List<Customer> findByCountryCodeAndAccountRole(String countryCode, Role role);

  // Doesn't need to be in a @Transactional because it's does the JOIN FETCH in one query
  @Query(
      "SELECT c FROM Customer c JOIN FETCH c.account WHERE c.countryCode = :countryCode AND c.account.role = :role")
  List<Customer> findByCountryCodeAndAccountRoleWithQuery(
      @Param("countryCode") String countryCode, @Param("role") Role role);
}
