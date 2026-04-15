package com.lunisoft.javastarter.module.media.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "media")
public class Media extends BaseEntity {

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String key;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long size;
}
