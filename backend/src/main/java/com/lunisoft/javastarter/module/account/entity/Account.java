package com.lunisoft.javastarter.module.account.entity;

import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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

  private String providerId;

  private String providerAccountId;

  @Column(nullable = false)
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
