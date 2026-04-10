package com.lunisoft.javastarter.module.customer.repository;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByAccountId(UUID accountId);
}
