package com.lunisoft.ultimatejavastarterkit.module.media.entity;

import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "media")
public class Media extends BaseEntity {

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(nullable = false, length = 500)
  private String key;

  @Column(name = "mime_type", nullable = false)
  private String mimeType;

  @Column(nullable = false)
  private long size;
}
