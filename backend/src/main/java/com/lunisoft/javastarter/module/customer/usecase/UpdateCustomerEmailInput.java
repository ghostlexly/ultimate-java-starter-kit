package com.lunisoft.javastarter.module.customer.usecase;

import java.util.UUID;

public record UpdateCustomerEmailInput(UUID accountId, String email) {}
