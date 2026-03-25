package com.lunisoft.ultimatejavastarterkit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admin")
public class Admin extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private Account account;

  // --- Getters & Setters ---

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }
}
