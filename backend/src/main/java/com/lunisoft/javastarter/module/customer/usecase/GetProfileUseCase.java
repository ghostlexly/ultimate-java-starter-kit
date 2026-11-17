package com.lunisoft.javastarter.module.customer.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final CustomerRepository customerRepository;

    public record Output(UUID id, String email) {}

    @Transactional(readOnly = true)
    public Output execute(UUID accountId) {
        Customer customer = customerRepository
                .findByAccountId(accountId)
                .orElseThrow(() ->
                        new BusinessRuleException("Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        return new Output(customer.getId(), customer.getAccount().getEmail());
    }
}
