package com.lunisoft.ultimatejavastarterkit.repository;

import com.lunisoft.ultimatejavastarterkit.entity.VerificationToken;
import com.lunisoft.ultimatejavastarterkit.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

  Optional<VerificationToken> findFirstByAccountIdAndTypeOrderByCreatedAtDesc(
      UUID accountId, VerificationType type);

  Optional<VerificationToken> findFirstByAccountIdAndTypeAndExpiresAtAfterOrderByCreatedAtDesc(
      UUID accountId, VerificationType type, Instant now);

  void deleteByExpiresAtBefore(Instant now);
}
