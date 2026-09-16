package com.lunisoft.javastarter.module.media.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@Entity
@Getter
@Setter
@NullMarked
public class Media extends BaseEntity {

    protected Media() {}

    public Media(String fileName, String key, String mimeType, long size) {
        this.fileName = Objects.requireNonNull(fileName);
        this.key = Objects.requireNonNull(key);
        this.mimeType = Objects.requireNonNull(mimeType);
        this.size = size;
    }

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String key;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long size;
}
