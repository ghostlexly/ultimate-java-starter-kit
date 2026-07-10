package com.lunisoft.javastarter.core.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated API response envelope.
 *
 * <p>Shape: {@code { content, totalItems, totalPages, isFirst, isLast }} (see java-backend
 * CLAUDE.md). Build it from a Spring Data {@link Page} via {@link #from(Page)}.
 */
public record PaginatedResponse<T>(List<T> content, long totalItems, int totalPages, boolean isFirst, boolean isLast) {

    public static <T> PaginatedResponse<T> from(Page<T> page) {

        return new PaginatedResponse<>(
                page.getContent(), page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
