package com.lunisoft.javastarter.module.appconfig.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@Getter
@Setter
@NullMarked
public class AppConfig extends BaseEntity {
    protected AppConfig() {}

    public AppConfig(String key) {
        this.key = Objects.requireNonNull(key);
    }

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = true)
    @Nullable
    private String value;
}
