package com.lunisoft.javastarter.module.customer.repository;

import com.lunisoft.javastarter.module.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByAccountId(UUID accountId);
}
