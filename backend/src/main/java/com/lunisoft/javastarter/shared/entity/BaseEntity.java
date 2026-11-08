package com.lunisoft.javastarter.shared.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity with shared fields: id, createdAt, updatedAt. All entities should extend this class.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Optimistic locking counter, managed by Hibernate. Incremented on every update: a flush whose
     * row version no longer matches throws {@code OptimisticLockException}, preventing lost updates
     * when two transactions modify the same entity concurrently.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
