package com.lunisoft.javastarter.shared;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
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

  private TestFactory() {}

  // ── Account ──────────────────────────────────────────────

  public static Account createAccount(String email, Role role) {
    var account = new Account();
    account.setId(UUID.randomUUID());
    account.setEmail(email);
    account.setRole(role);

    return account;
  }

  public static Account createAccount(String email) {

    return createAccount(email, Role.CUSTOMER);
  }

  public static Account createAccount(UUID id, String email) {
    var account = createAccount(email, Role.CUSTOMER);
    account.setId(id);

    return account;
  }

  // ── Customer ─────────────────────────────────────────────

  public static Customer createCustomer(Account account) {
    var customer = new Customer();
    customer.setId(UUID.randomUUID());
    customer.setAccount(account);

    return customer;
  }

  public static Customer createCustomer(String email, Role role) {
    var account = createAccount(email, role);

    return createCustomer(account);
  }

  // ── Session ──────────────────────────────────────────────

  public static Session createSession(Account account) {
    var session = new Session();
    session.setId(UUID.randomUUID());
    session.setAccount(account);
    session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

    return session;
  }

  public static Session createSession(UUID sessionId, Account account) {
    var session = createSession(account);
    session.setId(sessionId);

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
