package com.lunisoft.javastarter.core.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PaginationService {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 100;

    /**
     * Common pagination query parameters.
     *
     * <p>Pages are 1-based in the API ({@code ?page=1}) and converted to Spring Data's 0-based index.
     * Defaults: {@code page=1}, {@code size=50}, capped at {@code 100} items per page.
     */
    public Pageable toPageable(Integer page, Integer size, Sort sort) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(currentPage - 1, pageSize, sort);
    }

    public Sort resolveSort(
            Map<String, List<String>> sortableProperties, Sort defaultSort, String sortInput, String orderInput) {
        List<String> properties = sortInput == null ? null : sortableProperties.get(sortInput);

        if (properties == null) {
            return defaultSort;
        }

        var direction = "desc".equalsIgnoreCase(orderInput) ? Sort.Direction.DESC : Sort.Direction.ASC;

        return Sort.by(direction, properties.toArray(String[]::new));
    }
}
