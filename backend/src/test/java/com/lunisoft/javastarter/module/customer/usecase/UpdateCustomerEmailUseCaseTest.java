package com.lunisoft.javastarter.module.customer.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.javastarter.module.customer.event.CustomerEmailUpdatedEvent;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import com.lunisoft.javastarter.shared.builder.AccountBuilder;
import com.lunisoft.javastarter.shared.builder.CustomerBuilder;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerEmailUseCaseTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private UpdateCustomerEmailUseCase updateCustomerEmailUseCase;

  @BeforeEach
  void setUp() {
    updateCustomerEmailUseCase =
        new UpdateCustomerEmailUseCase(customerRepository, accountRepository, eventPublisher);
  }

  @Test
  void execute_validRequest_updatesEmailAndPublishesEvent() {
    var accountId = UUID.randomUUID();
    var account = new AccountBuilder().id(accountId).email("old@example.com").build();
    var customer = new CustomerBuilder().account(account).build();
    var request = new UpdateCustomerEmailRequest("new@example.com");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));

    var result = updateCustomerEmailUseCase.execute(accountId, request);

    assertThat(result.email()).isEqualTo("new@example.com");
    assertThat(account.getEmail()).isEqualTo("new@example.com");

    var eventCaptor = ArgumentCaptor.forClass(CustomerEmailUpdatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().customer()).isEqualTo(customer);
    assertThat(eventCaptor.getValue().newEmail()).isEqualTo("new@example.com");
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
    var request = new UpdateCustomerEmailRequest("new@example.com");

    assertThatThrownBy(() -> updateCustomerEmailUseCase.execute(accountId, request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void execute_customerNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    var account = new AccountBuilder().id(accountId).build();
    var request = new UpdateCustomerEmailRequest("new@example.com");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateCustomerEmailUseCase.execute(accountId, request))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
            });

    verify(eventPublisher, never()).publishEvent(any());
  }
}
