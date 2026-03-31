package com.lunisoft.ultimatejavastarterkit.module.account.repository;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  Optional<Account> findByEmail(String email);
}
