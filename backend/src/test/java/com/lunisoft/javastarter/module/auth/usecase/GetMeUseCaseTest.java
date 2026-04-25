package com.lunisoft.javastarter.module.auth.usecase;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMeUseCaseTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private GetMeUseCase getMeUseCase;

  @Test
  void execute_existingAccount_returnsMeResponse() {
    Account account = createCustomerAccount();
    var accountId = account.getId();

    when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

    var result = getMeUseCase.execute(accountId);

    assertThat(result.accountId()).isEqualTo(accountId);
    assertThat(result.email()).isEqualTo(account.getEmail());
    assertThat(result.role()).isEqualTo(account.getRole().name());
  }

  @Test
  void execute_accountNotFound_throwsBusinessRuleException() {
    var accountId = UUID.randomUUID();
    when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getMeUseCase.execute(accountId))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Account not found");
  }
}
