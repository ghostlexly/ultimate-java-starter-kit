package com.lunisoft.ultimatejavastarterkit.module.auth.entity;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "verification_token")
public class VerificationToken extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VerificationType type;

  private String value;

  @Column(nullable = false)
  private int attempts = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public VerificationType getType() {
    return type;
  }

  public void setType(VerificationType type) {
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public int getAttempts() {
    return attempts;
  }

  public void setAttempts(int attempts) {
    this.attempts = attempts;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }
}
