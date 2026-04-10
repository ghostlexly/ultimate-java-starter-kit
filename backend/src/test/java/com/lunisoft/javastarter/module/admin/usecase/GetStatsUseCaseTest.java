package com.lunisoft.javastarter.module.admin.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lunisoft.javastarter.module.account.repository.AccountRepository;
import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStatsUseCaseTest {

  @Mock private AccountRepository accountRepository;
  @Mock private SessionRepository sessionRepository;

  private GetStatsUseCase getStatsUseCase;

  @BeforeEach
  void setUp() {
    getStatsUseCase = new GetStatsUseCase(accountRepository, sessionRepository);
  }

  @Test
  void execute_returnsAccountsAndSessionCounts() {
    when(accountRepository.count()).thenReturn(42L);
    when(sessionRepository.count()).thenReturn(7L);

    var result = getStatsUseCase.execute();

    assertThat(result).containsEntry("accounts", 42L).containsEntry("activeSessions", 7L);
  }

  @Test
  void execute_zeroCounts_returnsZeros() {
    when(accountRepository.count()).thenReturn(0L);
    when(sessionRepository.count()).thenReturn(0L);

    var result = getStatsUseCase.execute();

    assertThat(result).containsEntry("accounts", 0L).containsEntry("activeSessions", 0L);
  }
}
