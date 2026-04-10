package com.lunisoft.javastarter.module.admin.usecase;

import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
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
