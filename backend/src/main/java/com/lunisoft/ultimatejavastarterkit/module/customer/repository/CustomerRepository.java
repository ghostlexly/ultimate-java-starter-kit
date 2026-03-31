package com.lunisoft.ultimatejavastarterkit.module.customer.repository;

import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByAccountId(UUID accountId);
}
