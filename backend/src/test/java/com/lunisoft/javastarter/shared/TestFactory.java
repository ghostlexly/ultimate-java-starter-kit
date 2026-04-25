package com.lunisoft.javastarter.shared;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Shared factory methods for creating test entities. Avoids duplicating helper methods across test
 * classes.
 */
public final class TestFactory {

  // ── Account ──────────────────────────────────────────────

  public static Account createCustomerAccount() {
    var account = new Account();
    account.setId(UUID.randomUUID());
    account.setEmail("contact+customer@lunisoft.fr");
    account.setRole(Role.CUSTOMER);

    var customer = createCustomer(account);
    account.setCustomer(customer);

    return account;
  }

  public static Account createAdminAccount() {
    var account = new Account();
    account.setId(UUID.randomUUID());
    account.setEmail("contact+admin@lunisoft.fr");
    account.setRole(Role.ADMIN);

    var admin = new Admin();
    account.setAdmin(admin);

    return account;
  }

  // ── Customer ─────────────────────────────────────────────

  public static Customer createCustomer(Account account) {
    var customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setAccount(account);

    return customer;
  }

  // ── Session ──────────────────────────────────────────────

  public static Session createSession(Account account) {
    var session = new Session();
    session.setId(UUID.randomUUID());
    session.setAccount(account);
    session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

    return session;
  }

  // ── VerificationToken ────────────────────────────────────

  public static VerificationToken createVerificationToken(
      Account account, String code, int attempts) {
    var token = new VerificationToken();
    token.setId(UUID.randomUUID());
    token.setToken(UUID.randomUUID().toString());
    token.setType(VerificationType.LOGIN_CODE);
    token.setValue(code);
    token.setAccount(account);
    token.setAttempts(attempts);
    token.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));

    return token;
  }
}
