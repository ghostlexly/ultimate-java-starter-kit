package com.lunisoft.javastarter.module.admin.usecase;

import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStatsUseCase {

    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;

    public record Output(long accounts, long activeSessions) {}

    public Output execute() {
        return new Output(accountRepository.count(), sessionRepository.count());
    }
}
