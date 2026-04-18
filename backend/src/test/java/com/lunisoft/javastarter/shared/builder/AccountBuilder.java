package com.lunisoft.javastarter.shared.builder;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import java.util.UUID;

public final class AccountBuilder {

  private UUID id = UUID.randomUUID();
  private String email = "test@example.com";
  private Role role = Role.CUSTOMER;
  private boolean emailVerified = false;

  public AccountBuilder id(UUID id) {
    this.id = id;

    return this;
  }

  public AccountBuilder email(String email) {
    this.email = email;

    return this;
  }

  public AccountBuilder role(Role role) {
    this.role = role;

    return this;
  }

  public AccountBuilder emailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;

    return this;
  }

  public Account build() {
    var account = new Account();
    account.setId(id);
    account.setEmail(email);
    account.setRole(role);
    account.setEmailVerified(emailVerified);

    return account;
  }
}
