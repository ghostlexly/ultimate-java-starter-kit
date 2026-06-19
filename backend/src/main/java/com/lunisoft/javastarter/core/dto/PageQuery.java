package com.lunisoft.javastarter.core.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Common pagination query parameters.
 *
 * <p>Pages are 1-based in the API ({@code ?page=1}) and converted to Spring Data's 0-based index.
 * Defaults: {@code page=1}, {@code size=50}, capped at {@code 100} items per page.
 */
public record PageQuery(Integer page, Integer size) {

  private static final int DEFAULT_SIZE = 50;
  private static final int MAX_SIZE = 100;

  public Pageable toPageable(Sort sort) {
    int currentPage = (page == null || page < 1) ? 1 : page;
    int pageSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

    return PageRequest.of(currentPage - 1, pageSize, sort);
  }
}
