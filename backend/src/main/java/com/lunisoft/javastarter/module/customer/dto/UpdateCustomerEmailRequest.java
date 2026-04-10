package com.lunisoft.javastarter.module.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerEmailRequest(@NotBlank @Email String email) {}
