package com.lunisoft.javastarter.module.media.entity;

import com.lunisoft.javastarter.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@Entity
@Getter
@Setter
@NullMarked
public class Media extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String key;

    @Column(nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private long size;
}
