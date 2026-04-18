package com.lunisoft.javastarter.shared.builder;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class VerificationTokenBuilder {

  private UUID id = UUID.randomUUID();
  private String token = UUID.randomUUID().toString();
  private VerificationType type = VerificationType.LOGIN_CODE;
  private String value = "1234";
  private int attempts = 0;
  private Account account = new AccountBuilder().build();
  private Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

  public VerificationTokenBuilder id(UUID id) {
    this.id = id;

    return this;
  }

  public VerificationTokenBuilder token(String token) {
    this.token = token;

    return this;
  }

  public VerificationTokenBuilder type(VerificationType type) {
    this.type = type;

    return this;
  }

  public VerificationTokenBuilder value(String value) {
    this.value = value;

    return this;
  }

  public VerificationTokenBuilder attempts(int attempts) {
    this.attempts = attempts;

    return this;
  }

  public VerificationTokenBuilder account(Account account) {
    this.account = account;

    return this;
  }

  public VerificationTokenBuilder expiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;

    return this;
  }

  public VerificationToken build() {
    var verificationToken = new VerificationToken();
    verificationToken.setId(id);
    verificationToken.setToken(token);
    verificationToken.setType(type);
    verificationToken.setValue(value);
    verificationToken.setAttempts(attempts);
    verificationToken.setAccount(account);
    verificationToken.setExpiresAt(expiresAt);

    return verificationToken;
  }
}
