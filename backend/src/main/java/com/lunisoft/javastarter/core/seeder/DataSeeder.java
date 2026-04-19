package com.lunisoft.javastarter.core.seeder;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds essential bootstrap data on every startup (all environments). Idempotent — checks if data
 * already exists before inserting. Runs before DevDataSeeder via @Order(1).
 */
@Component
@RequiredArgsConstructor
@Order(1) // Run this DataSeeder before DevDataSeeder
public class DataSeeder implements ApplicationRunner {

  private static final String ADMIN_EMAIL = "admin@lunisoft.fr";
  private static final String SEED_PASSWORD = "password";

  private final Logger log = LoggerFactory.getLogger(DataSeeder.class);

  private final AccountRepository accountRepository;
  private final AdminRepository adminRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(@NonNull ApplicationArguments args) {
    seedAdminAccount();
    log.info("Production data seeding complete.");
  }

  // ── Admin account ──────────────────────────────────────────────────────────

  private void seedAdminAccount() {
    if (accountRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
      log.info("Admin account already exists, skipping.");

      return;
    }

    var account = new Account();
    account.setEmail(ADMIN_EMAIL);
    account.setPassword(passwordEncoder.encode(SEED_PASSWORD));
    account.setRole(Role.ADMIN);
    account.setEmailVerified(true);
    accountRepository.save(account);

    var admin = new Admin();
    admin.setAccount(account);
    adminRepository.save(admin);

    log.info("Seeded admin account: {}", ADMIN_EMAIL);
  }
}
