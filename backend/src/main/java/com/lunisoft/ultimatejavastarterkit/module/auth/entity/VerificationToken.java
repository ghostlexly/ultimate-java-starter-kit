package com.lunisoft.ultimatejavastarterkit.module.auth.entity;

import com.lunisoft.ultimatejavastarterkit.module.account.entity.Account;
import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "verification_token")
public class VerificationToken extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VerificationType type;

  private String value;

  @Column(nullable = false)
  private int attempts = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
