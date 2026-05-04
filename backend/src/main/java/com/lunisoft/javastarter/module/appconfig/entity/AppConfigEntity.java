package com.lunisoft.javastarter.module.appconfig.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "app_configs")
public class AppConfigEntity extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String key;

  private String value;
}
