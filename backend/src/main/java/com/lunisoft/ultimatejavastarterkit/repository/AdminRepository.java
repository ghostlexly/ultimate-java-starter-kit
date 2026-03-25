package com.lunisoft.ultimatejavastarterkit.repository;

import com.lunisoft.ultimatejavastarterkit.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
  Optional<Admin> findByAccountId(UUID accountId);
}
