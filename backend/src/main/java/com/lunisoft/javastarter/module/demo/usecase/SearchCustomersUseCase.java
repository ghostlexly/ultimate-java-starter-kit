package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.demo.repository.DemoCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Demo use case: searches customers by account role. Illustrates how to query across a join
 * (Customer -> Account).
 */
@Service
@RequiredArgsConstructor
public class SearchCustomersUseCase {

    private final DemoCustomerRepository demoCustomerRepository;

    public record SearchCustomersQuery(Role role) {}

    public record SearchCustomersResult(UUID id, String email, String role) {}

    @Transactional(readOnly = true)
    public List<SearchCustomersResult> execute(SearchCustomersQuery query) {
        return demoCustomerRepository.findByAccountRole(query.role()).stream()
                .map(customer -> new SearchCustomersResult(
                        customer.getId(),
                        customer.getAccount().getEmail(),
                        customer.getAccount().getRole().name()))
                .toList();
    }
}
