package com.lunisoft.javastarter.shared.builder;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.auth.entity.Session;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class SessionBuilder {

  private UUID id = UUID.randomUUID();
  private Account account = new AccountBuilder().build();
  private Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

  public SessionBuilder id(UUID id) {
    this.id = id;

    return this;
  }

  public SessionBuilder account(Account account) {
    this.account = account;

    return this;
  }

  public SessionBuilder expiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;

    return this;
  }

  public Session build() {
    var session = new Session();
    session.setId(id);
    session.setAccount(account);
    session.setExpiresAt(expiresAt);

    return session;
  }
}
