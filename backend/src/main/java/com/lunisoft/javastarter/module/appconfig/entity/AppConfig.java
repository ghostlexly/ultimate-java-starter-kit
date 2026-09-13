package com.lunisoft.javastarter.module.appconfig.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Getter
@Setter
public class AppConfig extends BaseEntity {
    protected AppConfig() {}

    public AppConfig(String key) {
        this.key = Objects.requireNonNull(key);
    }

    @Column(nullable = false, unique = true)
    private String key;

    private String value;
}
