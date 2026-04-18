package com.lunisoft.javastarter.shared.builder;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import java.util.UUID;

public final class CustomerBuilder {

  private UUID id = UUID.randomUUID();
  private Account account = new AccountBuilder().build();

  public CustomerBuilder id(UUID id) {
    this.id = id;

    return this;
  }

  public CustomerBuilder account(Account account) {
    this.account = account;

    return this;
  }

  public Customer build() {
    var customer = new Customer();
    customer.setId(id);
    customer.setAccount(account);

    return customer;
  }
}
