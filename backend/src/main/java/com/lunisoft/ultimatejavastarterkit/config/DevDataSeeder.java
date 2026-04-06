package com.lunisoft.ultimatejavastarterkit.config;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with sample data on startup. Only active in the "dev" profile. Runs after
 * ProductionDataSeeder via @Order(2). Checks if data already exists before inserting to remain
 * idempotent.
 */
@Component
@Profile("dev")
@Order(2) // Run this DevDataSeeder after ProductionDataSeeder
public class DevDataSeeder implements ApplicationRunner {

  private static final String SEED_PASSWORD = "password";

  private final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

  private final AccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  public DevDataSeeder(
      AccountRepository accountRepository,
      CustomerRepository customerRepository,
      PasswordEncoder passwordEncoder) {
    this.accountRepository = accountRepository;
    this.customerRepository = customerRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(@NonNull ApplicationArguments args) {
    seedCustomerAccount();
    log.info("Dev data seeding complete.");
  }

  // ── Customer account with addresses ────────────────────────────────────────

  private void seedCustomerAccount() {
    var email = "customer@lunisoft.fr";

    if (accountRepository.findByEmail(email).isPresent()) {
      log.info("Customer account already exists, skipping.");

      return;
    }

    // Account
    var account = new Account();
    account.setEmail(email);
    account.setPassword(passwordEncoder.encode(SEED_PASSWORD));
    account.setRole(Role.CUSTOMER);
    account.setEmailVerified(true);
    accountRepository.save(account);

    // Customer
    var customer = new Customer();
    customer.setAccount(account);
    customer.setCountryCode("FR");
    customerRepository.save(customer);

    customerRepository.save(customer);

    log.info("Seeded customer account: {}", email);
  }
}
