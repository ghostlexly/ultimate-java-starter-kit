package com.lunisoft.ultimatejavastarterkit.repository;

import com.lunisoft.ultimatejavastarterkit.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
  Optional<Customer> findByAccountId(UUID accountId);
}
