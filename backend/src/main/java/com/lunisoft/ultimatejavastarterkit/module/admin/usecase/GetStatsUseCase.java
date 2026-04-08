package com.lunisoft.ultimatejavastarterkit.module.admin.usecase;

import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.SessionRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStatsUseCase {

  private final AccountRepository accountRepository;
  private final SessionRepository sessionRepository;

  public Map<String, Long> execute() {

    return Map.of(
        "accounts", accountRepository.count(),
        "activeSessions", sessionRepository.count());
  }
}
