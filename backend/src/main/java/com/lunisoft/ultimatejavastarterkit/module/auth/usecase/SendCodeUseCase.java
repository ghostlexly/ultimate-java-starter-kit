package com.lunisoft.ultimatejavastarterkit.module.auth.usecase;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.module.account.entity.Role;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.auth.AuthConstants;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.VerificationToken;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.VerificationType;
import com.lunisoft.ultimatejavastarterkit.module.auth.event.LoginCodeRequestedEvent;
import com.lunisoft.ultimatejavastarterkit.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendCodeUseCase {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AccountRepository accountRepository;
  private final CustomerRepository customerRepository;
  private final VerificationTokenRepository verificationTokenRepository;
  private final ApplicationEventPublisher eventPublisher;

  public SendCodeUseCase(
      AccountRepository accountRepository,
      CustomerRepository customerRepository,
      VerificationTokenRepository verificationTokenRepository,
      ApplicationEventPublisher eventPublisher) {
    this.accountRepository = accountRepository;
    this.customerRepository = customerRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * Generates a 4-digit login code and stores it as a VerificationToken. Creates the account if it
   * doesn't exist (defaults to CUSTOMER role).
   */
  @Transactional
  public void execute(String email) {
    Account account =
        accountRepository
            .findByEmail(email)
            .orElseGet(
                () -> {
                  Account newAccount = new Account();
                  newAccount.setEmail(email);
                  newAccount.setRole(Role.CUSTOMER);
                  Account savedAccount = accountRepository.save(newAccount);

                  Customer newCustomer = new Customer();
                  newCustomer.setAccount(savedAccount);
                  customerRepository.save(newCustomer);

                  return savedAccount;
                });

    // Enforce cooldown between code requests
    verificationTokenRepository
        .findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE)
        .ifPresent(this::checkCooldown);

    String code = "%04d".formatted(SECURE_RANDOM.nextInt(10000));

    VerificationToken token = new VerificationToken();
    token.setToken(UUID.randomUUID().toString());
    token.setType(VerificationType.LOGIN_CODE);
    token.setValue(code);
    token.setAccount(account);
    token.setExpiresAt(
        Instant.now().plus(AuthConstants.LOGIN_CODE_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
    verificationTokenRepository.save(token);

    eventPublisher.publishEvent(new LoginCodeRequestedEvent(email, code));
  }

  private void checkCooldown(VerificationToken lastToken) {
    long secondsSince = Duration.between(lastToken.getCreatedAt(), Instant.now()).toSeconds();

    if (secondsSince < AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS) {
      long remaining = AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS - secondsSince;
      throw new BusinessRuleException(
          "Please wait %s seconds before requesting a new code.".formatted(remaining),
          "LOGIN_CODE_COOLDOWN",
          HttpStatus.TOO_MANY_REQUESTS);
    }
  }
}
