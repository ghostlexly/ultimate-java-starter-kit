package com.lunisoft.javastarter.module.auth.repository;

import com.lunisoft.javastarter.module.auth.entity.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, UUID> {
  Optional<Session> findByIdAndExpiresAtAfter(UUID id, Instant now);

  boolean existsByIdAndExpiresAtAfter(UUID id, Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
