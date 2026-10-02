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

    public record UpdateCustomerEmailCommand(UUID accountId, String email) {}

    public record UpdateCustomerEmailResult(UUID id) {}

    @Transactional
    public UpdateCustomerEmailResult execute(UpdateCustomerEmailCommand command) {
        Account account = accountRepository
                .findById(command.accountId())
                .orElseThrow(() -> new BusinessRuleException("Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        Customer customer = customerRepository
                .findByAccountId(command.accountId())
                .orElseThrow(() ->
                        new BusinessRuleException("Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        // Update the email
        account.setEmail(command.email());

        // Publish event so listeners can react (e.g. auto-detect country code)
        eventPublisher.publishEvent(new CustomerEmailUpdatedEvent(customer, command.email()));

        return new UpdateCustomerEmailResult(customer.getId());
    }
}
