package com.lunisoft.javastarter.module.admin.entity;

import com.lunisoft.javastarter.module.account.entity.Account;
import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
public class Admin extends BaseEntity {
    protected Admin() {}

    public Admin(Account account) {
        this.account = Objects.requireNonNull(account);
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
}
