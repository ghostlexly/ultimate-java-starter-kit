package com.lunisoft.javastarter.module.demo.repository;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DemoCustomerRepository
        extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    // Needs to be in a @Transactional to loads the account relation which is set as Lazy
    List<Customer> findByAccountRole(Role role);

    // Doesn't need to be in a @Transactional because it's does the JOIN FETCH in one query
    @Query("""
            SELECT c FROM Customer c
            JOIN c.account
            WHERE c.account.role = :role
            """)
    List<Customer> findByAccountRoleWithQuery(@Param("role") Role role);
}
