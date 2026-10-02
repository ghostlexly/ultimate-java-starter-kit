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

    public record GetMeQuery(UUID accountId) {}

    public record GetMeResult(UUID accountId, String email, String role) {}

    public GetMeResult execute(GetMeQuery query) {
        Account account = accountRepository
                .findById(query.accountId())
                .orElseThrow(() -> new BusinessRuleException("Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

        return new GetMeResult(account.getId(), account.getEmail(), account.getRole().name());
    }
}
