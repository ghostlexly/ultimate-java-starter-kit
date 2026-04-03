package com.lunisoft.ultimatejavastarterkit.module.customer.usecase;

import static com.lunisoft.ultimatejavastarterkit.shared.TestFactory.createAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lunisoft.ultimatejavastarterkit.core.exception.BusinessRuleException;
import com.lunisoft.ultimatejavastarterkit.module.account.repository.AccountRepository;
import com.lunisoft.ultimatejavastarterkit.module.customer.dto.RegisterCustomerRequest;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.module.customer.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CreateProfileUseCaseTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private AccountRepository accountRepository;

  private CreateProfileUseCase createProfileUseCase;

  @BeforeEach
  void setUp() {
    createProfileUseCase = new CreateProfileUseCase(customerRepository, accountRepository);
  }

  @Test
  void execute_validRequest_createsProfile() {
    var accountId = UUID.randomUUID();
    var account = createAccount(accountId, "test@example.com");
    var request = new RegisterCustomerRequest("US");

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
    when(customerRepository.save(any(Customer.class)))
        .thenAnswer(
            invocation -> {
              var customer = invocation.getArgument(0, Customer.class);
              customer.setId(UUID.randomUUID());

              return customer;
            });

    var result = createProfileUseCase.execute(accountId, request);

    assertThat(result.email()).isEqualTo("test@example.com");
    assertThat(result.countryCode()).isEqualTo("US");

    var customerCaptor = ArgumentCaptor.forClass(Customer.class);
    verify(customerRepository).save(customerCaptor.capture());
    assertThat(customerCaptor.getValue().getCountryCode()).isEqualTo("US");
    assertThat(customerCaptor.getValue().getAccount()).isEqualTo(account);
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> createProfileUseCase.execute(accountId, new RegisterCustomerRequest("US")))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }

  @Test
  void execute_profileAlreadyExists_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    var account = createAccount(accountId, "test@example.com");
    var existingCustomer = new Customer();

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(existingCustomer));

    assertThatThrownBy(
            () -> createProfileUseCase.execute(accountId, new RegisterCustomerRequest("US")))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("ALREADY_EXISTS");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

    verify(customerRepository, never()).save(any());
  }

}
