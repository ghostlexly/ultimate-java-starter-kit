package com.lunisoft.javastarter.module.appconfig.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AppConfig extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String key;

    private String value;
}
