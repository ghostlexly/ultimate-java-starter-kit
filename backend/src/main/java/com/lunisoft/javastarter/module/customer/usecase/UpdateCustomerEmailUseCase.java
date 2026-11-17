package com.lunisoft.javastarter.module.customer.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.customer.event.CustomerEmailUpdatedEvent;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateCustomerEmailUseCase {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public record Output(UUID id) {}

    @Transactional
    public Output execute(UUID accountId, String email) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        Customer customer = customerRepository
                .findByAccountId(accountId)
                .orElseThrow(() ->
                        new BusinessRuleException("Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        // Update the email
        account.setEmail(email);

        // Publish event so listeners can react (e.g. auto-detect country code)
        eventPublisher.publishEvent(new CustomerEmailUpdatedEvent(customer, email));

        return new Output(customer.getId());
    }
}
