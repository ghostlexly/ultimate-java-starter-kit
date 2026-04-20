package com.lunisoft.javastarter.module.demo.usecase.paginatecustomer;

import java.util.List;
import java.util.UUID;

/**
 * Paginated response containing a list of customers and pagination metadata. Example: GET
 * /api/demo/customers/paginated?page=1&size=10&email=john
 */
public record DemoPaginateCustomerResult(
    List<CustomerItem> content, long totalItems, int totalPages, boolean isFirst, boolean isLast) {

  public record CustomerItem(UUID id, String email, String role) {}
}
