package com.lunisoft.ultimatejavastarterkit.module.auth.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.auth.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Returns the current authenticated user's info. */
@RequiredArgsConstructor
@Service
public class GetMeUseCase {

  private final AccountRepository accountRepository;

  public MeResponse execute(UUID accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "Account not found.", "NOT_FOUND", HttpStatus.NOT_FOUND));

    return new MeResponse(account.getId(), account.getEmail(), account.getRole().name());
  }
}
