package com.lunisoft.javastarter.core.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationServiceTest {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private static final Map<String, List<String>> SORTABLE_PROPERTIES = Map.of(
            "name", List.of("lastName", "firstName"),
            "email", List.of("account.email"));

    private final PaginationService paginationService = new PaginationService();

    @Test
    void toPageable_converts_one_based_page_to_zero_based_index() {
        var pageable = paginationService.toPageable(3, 20, DEFAULT_SORT);

        assertThat(pageable).isEqualTo(PageRequest.of(2, 20, DEFAULT_SORT));
    }

    @Test
    void toPageable_null_or_invalid_page_defaults_to_first_page() {
        assertThat(paginationService.toPageable(null, 10, DEFAULT_SORT).getPageNumber()).isZero();
        assertThat(paginationService.toPageable(0, 10, DEFAULT_SORT).getPageNumber()).isZero();
        assertThat(paginationService.toPageable(-5, 10, DEFAULT_SORT).getPageNumber()).isZero();
    }

    @Test
    void toPageable_null_or_invalid_size_defaults_to_fifty() {
        assertThat(paginationService.toPageable(1, null, DEFAULT_SORT).getPageSize()).isEqualTo(50);
        assertThat(paginationService.toPageable(1, 0, DEFAULT_SORT).getPageSize()).isEqualTo(50);
        assertThat(paginationService.toPageable(1, -1, DEFAULT_SORT).getPageSize()).isEqualTo(50);
    }

    @Test
    void toPageable_caps_size_at_one_hundred() {
        assertThat(paginationService.toPageable(1, 500, DEFAULT_SORT).getPageSize()).isEqualTo(100);
    }

    @Test
    void resolveSort_maps_whitelisted_key_to_entity_properties_ascending_by_default() {
        var sort = paginationService.resolveSort(SORTABLE_PROPERTIES, DEFAULT_SORT, "name", null);

        assertThat(sort).isEqualTo(Sort.by(Sort.Direction.ASC, "lastName", "firstName"));
    }

    @Test
    void resolveSort_applies_descending_order_case_insensitively() {
        var sort = paginationService.resolveSort(SORTABLE_PROPERTIES, DEFAULT_SORT, "email", "DESC");

        assertThat(sort).isEqualTo(Sort.by(Sort.Direction.DESC, "account.email"));
    }

    @Test
    void resolveSort_unknown_key_falls_back_to_default_sort() {
        var sort = paginationService.resolveSort(SORTABLE_PROPERTIES, DEFAULT_SORT, "hackyColumn", "asc");

        assertThat(sort).isEqualTo(DEFAULT_SORT);
    }

    @Test
    void resolveSort_null_key_falls_back_to_default_sort() {
        var sort = paginationService.resolveSort(SORTABLE_PROPERTIES, DEFAULT_SORT, null, "desc");

        assertThat(sort).isEqualTo(DEFAULT_SORT);
    }
}
