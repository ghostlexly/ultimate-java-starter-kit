package com.lunisoft.ultimatejavastarterkit.module.auth.repository;

import com.lunisoft.ultimatejavastarterkit.module.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
  Optional<Session> findByIdAndExpiresAtAfter(UUID id, Instant now);

  boolean existsByIdAndExpiresAtAfter(UUID id, Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
