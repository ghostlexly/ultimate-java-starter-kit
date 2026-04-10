package com.lunisoft.javastarter.module.auth.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.entity.Role;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class GetMeUseCaseTest {

  @Mock private AccountRepository accountRepository;

  private GetMeUseCase getMeUseCase;

  @BeforeEach
  void setUp() {
    getMeUseCase = new GetMeUseCase(accountRepository);
  }

  @Test
  void execute_existingAccount_returnsMeResponse() {
    var accountId = UUID.randomUUID();
    var account = new Account();
    account.setId(accountId);
    account.setEmail("test@example.com");
    account.setRole(Role.CUSTOMER);

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    var result = getMeUseCase.execute(accountId);

    assertThat(result.accountId()).isEqualTo(accountId);
    assertThat(result.email()).isEqualTo("test@example.com");
    assertThat(result.role()).isEqualTo("CUSTOMER");
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getMeUseCase.execute(accountId))
        .isInstanceOf(BusinessRuleException.class)
        .satisfies(
            ex -> {
              var bre = (BusinessRuleException) ex;
              assertThat(bre.getCode()).isEqualTo("NOT_FOUND");
              assertThat(bre.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
  }
}
