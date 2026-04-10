package com.lunisoft.javastarter.module.auth.repository;

import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.auth.entity.VerificationType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

  Optional<VerificationToken> findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
      UUID accountId, VerificationType type);

  Optional<VerificationToken> findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
      UUID accountId, VerificationType type, Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
