package com.lunisoft.javastarter.module.auth.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.AuthConstants;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import com.lunisoft.javastarter.module.auth.event.LoginCodeRequestedEvent;
import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SendCodeUseCaseTest {

  @Mock private AccountRepository accountRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private VerificationTokenRepository verificationTokenRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private SendCodeUseCase sendCodeUseCase;

  @BeforeEach
  void setUp() {
    sendCodeUseCase =
        new SendCodeUseCase(
            accountRepository, customerRepository, verificationTokenRepository, eventPublisher);
  }

  @Test
  void execute_existingAccount_sendsCode() {
    Account account = createCustomerAccount();
    when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(account.getEmail());

    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
    // Should not create a new account
    verify(accountRepository, never()).save(any());
  }

  @Test
  void execute_newAccount_createsAccountAndCustomerThenSendsCode() {
    Account account = createCustomerAccount();
    when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.empty());
    when(accountRepository.save(any(Account.class))).thenReturn(account);
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(account.getEmail());

    // Verify account created with CUSTOMER role
    verify(accountRepository)
        .save(
            assertArg(
                createdAccount -> {
                  assertThat(createdAccount.getRole()).isEqualTo(Role.CUSTOMER);
                  assertThat(createdAccount.getEmail()).isEqualTo(account.getEmail());
                }));

    // Verify customer created
    verify(customerRepository).save(any());

    // Verify token saved and event published
    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
  }

  @Test
  void execute_cooldownNotExpired_throwsBusinessRuleException() {
    Account account = createCustomerAccount();
    String email = account.getEmail();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));

    // Last token was created 10 seconds ago (within 60s cooldown)
    var recentToken = new VerificationToken();
    recentToken.setCreatedAt(Instant.now().minusSeconds(10));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.of(recentToken));

    assertThatThrownBy(() -> sendCodeUseCase.execute(email))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            exception -> {
              assertThat(exception.getCode()).isEqualTo("LOGIN_CODE_COOLDOWN");
            });

    verify(verificationTokenRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void execute_cooldownExpired_sendsCodeSuccessfully() {
    Account account = createCustomerAccount();
    when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));

    // Last token was created 61 seconds ago (past 60s cooldown)
    var oldToken = new VerificationToken();
    oldToken.setCreatedAt(
        Instant.now().minusSeconds(AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS + 1));

    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.of(oldToken));

    sendCodeUseCase.execute(account.getEmail());

    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
  }

  @Test
  void execute_savesTokenWithCorrectFields() {
    Account account = createCustomerAccount();
    when(accountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(account.getEmail());

    verify(verificationTokenRepository)
        .save(
            assertArg(
                token -> {
                  assertThat(token.getAccount()).isEqualTo(account);
                  assertThat(token.getType()).isEqualTo(VerificationType.LOGIN_CODE);
                  assertThat(token.getToken()).isNotNull();
                  assertThat(token.getValue()).hasSize(4);
                  assertThat(token.getAttempts()).isZero();
                  assertThat(token.getExpiresAt()).isAfter(Instant.now());
                }));
  }
}
