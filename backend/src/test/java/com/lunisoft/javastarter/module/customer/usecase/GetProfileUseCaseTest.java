package com.lunisoft.javastarter.module.customer.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.customer.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GetProfileUseCaseTest {

  @Mock private CustomerRepository customerRepository;

  @InjectMocks private GetProfileUseCase getProfileUseCase;

  @Test
  void execute_existingProfile_returnsCustomerResponse() {
    var account = createCustomerAccount();
    var customer = account.getCustomer();

    when(customerRepository.findByAccountId(account.getId())).thenReturn(Optional.of(customer));

    var result = getProfileUseCase.execute(account.getId());

    assertThat(result.id()).isEqualTo(customer.getId());
    assertThat(result.email()).isEqualTo("contact+customer@lunisoft.fr");
  }

  @Test
  void execute_profileNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getProfileUseCase.execute(accountId))
        .isInstanceOfSatisfying(
            BusinessRuleException.class,
            exception -> {
              assertThat(exception.getMessage()).isEqualTo("Customer profile not found");
              assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
            });
  }
}
