package com.lunisoft.javastarter.module.admin.repository;

import com.lunisoft.javastarter.module.admin.entity.Admin;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, UUID> {
  Optional<Admin> findByAccountId(UUID accountId);
}
