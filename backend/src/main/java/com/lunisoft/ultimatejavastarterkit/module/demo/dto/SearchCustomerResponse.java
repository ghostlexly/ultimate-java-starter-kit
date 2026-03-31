package com.lunisoft.ultimatejavastarterkit.module.demo.dto;

import java.util.UUID;

public record SearchCustomerResponse(
    UUID id,
    String email,
    String countryCode,
    String role
) {}
