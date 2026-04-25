package com.lunisoft.javastarter.module.customer.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.customer.dto.UpdateCustomerEmailRequest;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.module.customer.event.CustomerEmailUpdatedEvent;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerEmailUseCaseTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private UpdateCustomerEmailUseCase updateCustomerEmailUseCase;

  @Test
  void execute_validRequest_updatesEmailAndPublishesEvent() {
    Account account = createCustomerAccount();
    var accountId = account.getId();
    Customer customer = createCustomer(account);
    var request = new UpdateCustomerEmailRequest("new@example.com");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));

    var result = updateCustomerEmailUseCase.execute(accountId, request);

    // Verify that the account's email was updated'
    assertThat(result.email()).isEqualTo("new@example.com");
    assertThat(account.getEmail()).isEqualTo("new@example.com");

    // Verify that the CustomerEmailUpdated event was published
    verify(eventPublisher)
        .publishEvent(
            (Object)
                assertArg(
                    event -> {
                      assertThat(event)
                          .isInstanceOfSatisfying(
                              CustomerEmailUpdatedEvent.class,
                              customerEmailUpdatedEvent -> {
                                assertThat(customerEmailUpdatedEvent.customer())
                                    .isEqualTo(customer);
                                assertThat(customerEmailUpdatedEvent.newEmail())
                                    .isEqualTo("new@example.com");
                              });
                    }));
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCustomerEmailUseCase.execute(
                    accountId, new UpdateCustomerEmailRequest("new@example.com")))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            exception -> {
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
            });

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void execute_customerNotFound_throwsBusinessRuleException() {
    Account account = createCustomerAccount();
    var accountId = account.getId();

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                updateCustomerEmailUseCase.execute(
                    accountId, new UpdateCustomerEmailRequest("new@example.com")))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            exception -> {
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

    verify(eventPublisher, never()).publishEvent(any());
  }
}
