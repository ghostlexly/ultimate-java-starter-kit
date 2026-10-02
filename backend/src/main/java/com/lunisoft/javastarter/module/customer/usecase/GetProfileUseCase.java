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

    public record GetProfileQuery(UUID accountId) {}

    public record GetProfileResult(UUID id, String email) {}

    @Transactional(readOnly = true)
    public GetProfileResult execute(GetProfileQuery query) {
        Customer customer = customerRepository
                .findByAccountId(query.accountId())
                .orElseThrow(() ->
                        new BusinessRuleException("Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        return new GetProfileResult(customer.getId(), customer.getAccount().getEmail());
    }
}
