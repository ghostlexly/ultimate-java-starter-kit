package com.lunisoft.ultimatejavastarterkit.module.customer.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.CustomerResponse;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.event.CustomerEmailUpdatedEvent;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCustomerEmailUseCase {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CustomerResponse execute(UUID accountId, UpdateCustomerEmailRequest request) {
        Account account =
            accountRepository
                .findById(accountId)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        Customer customer =
            customerRepository
                .findByAccountId(accountId)
                .orElseThrow(
                    () ->
                        new BusinessRuleException(
                            "Customer profile not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        // Update the email
        account.setEmail(request.email());

        // Publish event so listeners can react (e.g. auto-detect country code)
        eventPublisher.publishEvent(new CustomerEmailUpdatedEvent(customer, request.email()));

        return new CustomerResponse(customer.getId(), account.getEmail());
    }
}
