package com.lunisoft.ultimatejavastarterkit.module.admin;

import com.lunisoft.ultimatejavastarterkit.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.repository.SessionRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  private final AccountRepository accountRepository;
  private final SessionRepository sessionRepository;

  public AdminService(AccountRepository accountRepository, SessionRepository sessionRepository) {
    this.accountRepository = accountRepository;
    this.sessionRepository = sessionRepository;
  }

  public long getAccountCount() {
    return accountRepository.count();
  }

  public long getActiveSessionCount() {
    return sessionRepository.count();
  }
}
