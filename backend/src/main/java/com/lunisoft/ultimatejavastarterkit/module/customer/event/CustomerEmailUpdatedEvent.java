package com.lunisoft.ultimatejavastarterkit.module.customer.event;

import com.lunisoft.ultimatejavastarterkit.module.customer.entity.Customer;

/**
 * Published when a customer's email address is updated. Listeners can react to apply
 * side effects (e.g. auto-detecting country code) within the same transaction.
 */
public record CustomerEmailUpdatedEvent(Customer customer, String newEmail) {}
