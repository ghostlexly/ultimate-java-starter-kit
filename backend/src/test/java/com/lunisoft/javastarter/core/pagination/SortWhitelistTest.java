package com.lunisoft.javastarter.core.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SortWhitelistTest {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private static final Map<String, List<String>> SORTABLE_PROPERTIES = Map.of(
            "name", List.of("lastName", "firstName"),
            "email", List.of("account.email"));

    @Test
    void resolve_maps_whitelisted_key_to_entity_properties_with_requested_direction() {
        var sort = SortWhitelist.resolve(Sort.by(Sort.Order.desc("name")), SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(sort).isEqualTo(Sort.by(Sort.Direction.DESC, "lastName", "firstName"));
    }

    @Test
    void resolve_keeps_only_whitelisted_keys() {
        var requestedSort = Sort.by(Sort.Order.desc("name"), Sort.Order.asc("hackyColumn"), Sort.Order.asc("email"));

        var sort = SortWhitelist.resolve(requestedSort, SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(sort)
                .isEqualTo(Sort.by(
                        Sort.Order.desc("lastName"), Sort.Order.desc("firstName"), Sort.Order.asc("account.email")));
    }

    @Test
    void resolve_unknown_key_falls_back_to_default_sort() {
        var sort = SortWhitelist.resolve(Sort.by("hackyColumn"), SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(sort).isEqualTo(DEFAULT_SORT);
    }

    @Test
    void resolve_unsorted_falls_back_to_default_sort() {
        var sort = SortWhitelist.resolve(Sort.unsorted(), SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(sort).isEqualTo(DEFAULT_SORT);
    }

    @Test
    void apply_keeps_page_and_size_and_resolves_the_sort() {
        var requestedPageable = PageRequest.of(2, 20, Sort.by(Sort.Order.asc("email")));

        var pageable = SortWhitelist.apply(requestedPageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(pageable).isEqualTo(PageRequest.of(2, 20, Sort.by(Sort.Direction.ASC, "account.email")));
    }

    @Test
    void apply_without_whitelisted_key_uses_default_sort() {
        var requestedPageable = PageRequest.of(0, 10, Sort.by("hackyColumn"));

        var pageable = SortWhitelist.apply(requestedPageable, SORTABLE_PROPERTIES, DEFAULT_SORT);

        assertThat(pageable).isEqualTo(PageRequest.of(0, 10, DEFAULT_SORT));
    }
}
