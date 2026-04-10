package com.lunisoft.javastarter.module.account.repository;

import com.lunisoft.javastarter.module.account.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  Optional<Account> findByEmail(String email);
}
