package com.lunisoft.ultimatejavastarterkit.module.appconfig.entity;

import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_config")
public class AppConfigEntity extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String key;

  private String value;
}
