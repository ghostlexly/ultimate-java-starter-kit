package com.lunisoft.ultimatejavastarterkit.module.media.entity;

import com.lunisoft.ultimatejavastarterkit.shared.entity.BaseEntity;
import jakarta.persistence.*;
@Entity
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

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }
}
