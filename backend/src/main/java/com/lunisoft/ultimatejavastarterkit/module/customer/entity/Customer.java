package com.lunisoft.ultimatejavastarterkit.module.customer.entity;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
@Entity
@Table(name = "customer")
public class Customer extends BaseEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private Account account;

  @Column(name = "country_code")
  private String countryCode;

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }
}
