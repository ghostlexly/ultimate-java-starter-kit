package com.lunisoft.javastarter.module.auth.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Returns the current authenticated user's info.
 */
@Service
@RequiredArgsConstructor
public class GetMeUseCase {

    private final AccountRepository accountRepository;

    public record Output(UUID accountId, String email, String role) {}

    public Output execute(UUID accountId) {
        Account account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new BusinessRuleException("Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        return new Output(account.getId(), account.getEmail(), account.getRole().name());
    }
}
