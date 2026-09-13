package com.lunisoft.javastarter.module.auth.entity;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
public class VerificationToken extends BaseEntity {

    protected VerificationToken() {}

    public VerificationToken(String token, VerificationType type, Account account, Instant expiresAt) {
        this.token = Objects.requireNonNull(token);
        this.type = Objects.requireNonNull(type);
        this.account = Objects.requireNonNull(account);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

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

    @Column(nullable = false)
    private Instant expiresAt;
}
