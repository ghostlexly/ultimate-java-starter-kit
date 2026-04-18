package com.lunisoft.javastarter.module.auth.usecase;

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
import com.lunisoft.javastarter.shared.builder.AccountBuilder;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

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
    var email = "test@example.com";
    var account = new AccountBuilder().email(email).build();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(email);

    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
    // Should not create a new account
    verify(accountRepository, never()).save(any());
  }

  @Test
  void execute_newAccount_createsAccountAndCustomerThenSendsCode() {
    var email = "new@example.com";
    var savedAccount = new AccountBuilder().email(email).build();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());
    when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            savedAccount.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(email);

    // Verify account created with CUSTOMER role
    var accountCaptor = ArgumentCaptor.forClass(Account.class);
    verify(accountRepository).save(accountCaptor.capture());
    var createdAccount = accountCaptor.getValue();
    assert createdAccount.getRole() == Role.CUSTOMER;
    assert createdAccount.getEmail().equals(email);

    // Verify customer created
    verify(customerRepository).save(any());

    // Verify token saved and event published
    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
  }

  @Test
  void execute_cooldownNotExpired_throwsBusinessRuleException() {
    var email = "test@example.com";
    var account = new AccountBuilder().email(email).build();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));

    // Last token was created 10 seconds ago (within 60s cooldown)
    var recentToken = new VerificationToken();
    recentToken.setCreatedAt(Instant.now().minusSeconds(10));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.of(recentToken));

    assertThatThrownBy(() -> sendCodeUseCase.execute(email))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assert bre.getCode().equals("LOGIN_CODE_COOLDOWN");
              assert bre.getStatus() == HttpStatus.TOO_MANY_REQUESTS;
            });

    verify(verificationTokenRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void execute_cooldownExpired_sendsCodeSuccessfully() {
    var email = "test@example.com";
    var account = new AccountBuilder().email(email).build();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));

    // Last token was created 61 seconds ago (past 60s cooldown)
    var oldToken = new VerificationToken();
    oldToken.setCreatedAt(
        Instant.now().minusSeconds(AuthConstants.LOGIN_CODE_COOLDOWN_SECONDS + 1));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.of(oldToken));

    sendCodeUseCase.execute(email);

    verify(verificationTokenRepository).save(any(VerificationToken.class));
    verify(eventPublisher).publishEvent(any(LoginCodeRequestedEvent.class));
  }

  @Test
  void execute_savesTokenWithCorrectFields() {
    var email = "test@example.com";
    var account = new AccountBuilder().email(email).build();
    when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
    when(verificationTokenRepository.findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
            account.getId(), VerificationType.LOGIN_CODE))
        .thenReturn(Optional.empty());

    sendCodeUseCase.execute(email);

    var tokenCaptor = ArgumentCaptor.forClass(VerificationToken.class);
    verify(verificationTokenRepository).save(tokenCaptor.capture());
    var token = tokenCaptor.getValue();
    assert token.getType() == VerificationType.LOGIN_CODE;
    assert token.getAccount() == account;
    assert token.getToken() != null;
    assert token.getValue() != null && token.getValue().length() == 4;
    assert token.getExpiresAt() != null && token.getExpiresAt().isAfter(Instant.now());
  }
}
