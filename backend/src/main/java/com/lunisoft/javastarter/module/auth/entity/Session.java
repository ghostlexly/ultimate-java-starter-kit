package com.lunisoft.javastarter.module.auth.entity;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Setter
@NullMarked
public class Session extends BaseEntity {

    protected Session() {}

    public Session(Account account, Instant expiresAt) {
        this.account = Objects.requireNonNull(account);
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = true)
    @Nullable
    private String ipAddress;

    @Column(nullable = true)
    @Nullable
    private String userAgent;

    @Column(nullable = false)
    private Instant expiresAt;
}
