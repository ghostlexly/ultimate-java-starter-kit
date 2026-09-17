package com.lunisoft.javastarter.core.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Sort key whitelist resolution for client-provided sorting.
 *
 * <p>A web-resolved {@link Pageable} carries whatever {@code ?sort=} the client sent. It must never reach the
 * repository as-is: each use case declares its {@code SORTABLE_PROPERTIES} (API sort key → entity property paths)
 * and resolves the requested sort against it, so clients can never sort on arbitrary columns.
 */
public final class SortWhitelist {

    private SortWhitelist() {}

    /**
     * Returns the same page/size as the requested pageable, with its sort resolved against the whitelist.
     *
     * @see #resolve(Sort, Map, Sort)
     */
    public static Pageable apply(
            Pageable requestedPageable, Map<String, List<String>> sortableProperties, Sort defaultSort) {
        Sort sort = resolve(requestedPageable.getSort(), sortableProperties, defaultSort);

        return PageRequest.of(requestedPageable.getPageNumber(), requestedPageable.getPageSize(), sort);
    }

    /**
     * Keeps only the whitelisted API sort keys of the requested sort, each translated to its entity property paths
     * (the requested direction is preserved). Falls back to the default sort when no requested key is whitelisted.
     */
    public static Sort resolve(Sort requestedSort, Map<String, List<String>> sortableProperties, Sort defaultSort) {
        List<Sort.Order> orders = requestedSort.stream()
                .flatMap(order -> toWhitelistedOrders(order, sortableProperties))
                .toList();

        return orders.isEmpty() ? defaultSort : Sort.by(orders);
    }

    /** Translates one requested order into its entity property orders (none when the key is not whitelisted). */
    private static Stream<Sort.Order> toWhitelistedOrders(
            Sort.Order order, Map<String, List<String>> sortableProperties) {
        return sortableProperties.getOrDefault(order.getProperty(), List.of()).stream()
                .map(property -> new Sort.Order(order.getDirection(), property));
    }
}
