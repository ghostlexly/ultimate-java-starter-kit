package com.lunisoft.javastarter.shared;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.admin.repository.AdminRepository;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

/**
 * Persisted-entity fixtures for integration tests.
 *
 * <p>Lives in {@code src/test/java} so production component scanning never sees it. Inherits the
 * shared Spring context from {@link AbstractIntegrationTest}, so the repos point at the same
 * Testcontainers Postgres the rest of the test sees.
 *
 * <p>Every method follows the {@code givenX(...)} convention and persists what it builds (use
 * {@link TestFactory} when you want a detached entity). Each method has two overloads:
 *
 * <ul>
 *   <li><strong>Plain</strong>: sensible defaults, one-line setup for the happy path — {@code
 *       fixtures.givenCustomer("a@b.com")}.
 *   <li><strong>Customizer</strong>: same defaults, plus a {@link Consumer} that gets the
 *       freshly-built entity before it hits the DB — {@code fixtures.givenCustomer("a@b.com", a ->
 *       a.setEmailVerified(false))}. Override only what the test cares about; everything else stays
 *       default.
 * </ul>
 */
@TestComponent
@RequiredArgsConstructor
public class IntegrationTestFixtures {

  private final AccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final AdminRepository adminRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final SessionRepository sessionRepository;

  // ── Accounts ─────────────────────────────────────────────────────────────

  /** Customer account with the matching {@link Customer} row, both persisted and linked back. */
  public Account givenCustomer(String email) {

    return givenCustomer(email, _ -> {});
  }

  /** Same as {@link #givenCustomer(String)} but lets the test mutate the {@link Account} first. */
  public Account givenCustomer(String email, Consumer<Account> customizer) {
    var account = new Account();
    account.setEmail(email);
    account.setRole(Role.CUSTOMER);
    account.setEmailVerified(true);

    var customer = new Customer();
    customer.setAccount(account);
    account.setCustomer(customer);

    customizer.accept(account);

    accountRepository.save(account);
    customerRepository.save(customer);

    return account;
  }

  /** Admin account with the matching {@link Admin} row, both persisted and linked back. */
  public Account givenAdmin(String email) {
    return givenAdmin(email, _ -> {});
  }

  /** Same as {@link #givenAdmin(String)} but lets the test mutate the {@link Account} first. */
  public Account givenAdmin(String email, Consumer<Account> customizer) {
    var account = new Account();
    account.setEmail(email);
    account.setRole(Role.ADMIN);
    account.setEmailVerified(true);

    var admin = new Admin();
    admin.setAccount(account);
    account.setAdmin(admin);

    customizer.accept(account);

    accountRepository.save(account);
    adminRepository.save(admin);

    return account;
  }

  // ── Auth state ───────────────────────────────────────────────────────────

  /** Login code: never tried, 15-minute expiry. */
  public VerificationToken givenLoginCode(Account account, String code) {

    return givenLoginCode(account, code, _ -> {});
  }

  /**
   * Login code with the same defaults as {@link #givenLoginCode(Account, String)}, but lets the
   * test override attempts / expiry / anything else before persistence.
   */
  public VerificationToken givenLoginCode(
      Account account, String code, Consumer<VerificationToken> customizer) {

    var token = new VerificationToken();
    token.setToken(UUID.randomUUID().toString());
    token.setType(VerificationType.LOGIN_CODE);
    token.setValue(code);
    token.setAccount(account);
    token.setAttempts(0);
    token.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
    customizer.accept(token);

    return verificationTokenRepository.save(token);
  }

  /** Session with a 1-day expiry — enough for refresh-flow tests. */
  public Session givenSession(Account account) {

    return givenSession(account, _ -> {});
  }

  /** Same as {@link #givenSession(Account)} but lets the test override expiry / ip / user-agent. */
  public Session givenSession(Account account, Consumer<Session> customizer) {
    var session = new Session();
    session.setAccount(account);
    session.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
    customizer.accept(session);

    return sessionRepository.save(session);
  }
}
