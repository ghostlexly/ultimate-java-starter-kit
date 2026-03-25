package com.lunisoft.ultimatejavastarterkit.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_config")
public class AppConfigEntity extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String key;

  private String value;

  // --- Getters & Setters ---

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
