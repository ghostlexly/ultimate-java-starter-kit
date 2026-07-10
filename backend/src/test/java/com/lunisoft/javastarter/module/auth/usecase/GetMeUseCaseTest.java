package com.lunisoft.javastarter.module.auth.usecase;

import com.lunisoft.javastarter.core.exception.BusinessRuleException;
import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.lunisoft.javastarter.shared.TestFactory.createCustomerAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMeUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private GetMeUseCase getMeUseCase;

    @Test
    void execute_existing_account_returns_me_response() {
        Account account = createCustomerAccount();
        var accountId = account.getId();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        var output = getMeUseCase.execute(accountId);

        assertThat(output.accountId()).isEqualTo(accountId);
        assertThat(output.email()).isEqualTo(account.getEmail());
        assertThat(output.role()).isEqualTo(account.getRole().name());
    }

    @Test
    void execute_account_not_found_throws_business_rule_exception() {
        var accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMeUseCase.execute(accountId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Account not found.");
    }
}
