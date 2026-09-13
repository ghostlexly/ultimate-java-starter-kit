package com.lunisoft.javastarter.module.account.entity;

import com.lunisoft.javastarter.module.admin.entity.Admin;
import com.lunisoft.javastarter.module.auth.entity.Session;
import com.lunisoft.javastarter.module.auth.entity.VerificationToken;
import com.lunisoft.javastarter.module.customer.entity.Customer;
import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"provider_id", "provider_account_id", "role"})})
public class Account extends BaseEntity {

    protected Account() {}

    public Account(String email, Role role) {
        this.email = Objects.requireNonNull(email).toLowerCase();
        this.role = Objects.requireNonNull(role);
    }

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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<Session> sessions = new ArrayList<>();

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Customer customer;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private Admin admin;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    private List<VerificationToken> verificationTokens = new ArrayList<>();
}
