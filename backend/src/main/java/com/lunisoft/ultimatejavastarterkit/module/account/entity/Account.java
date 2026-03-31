package com.lunisoft.ultimatejavastarterkit.module.account.entity;

import com.lunisoft.ultimatejavastarterkit.module.admin.entity.Admin;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.Session;
import com.lunisoft.ultimatejavastarterkit.module.auth.entity.VerificationToken;
import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;
import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "account",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"provider_id", "provider_account_id", "role"})
    })
public class Account extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(name = "provider_id")
  private String providerId;

  @Column(name = "provider_account_id")
  private String providerAccountId;

  @Column(name = "is_email_verified", nullable = false)
  private boolean emailVerified = false;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Session> sessions;

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Customer customer;

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private Admin admin;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<VerificationToken> verificationTokens;
}
