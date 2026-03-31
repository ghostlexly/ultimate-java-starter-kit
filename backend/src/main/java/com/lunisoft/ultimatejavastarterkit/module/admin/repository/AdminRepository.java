package com.lunisoft.ultimatejavastarterkit.module.admin.repository;

import com.lunisoft.ultimatejavastarterkit.module.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
  Optional<Admin> findByAccountId(UUID accountId);
}
