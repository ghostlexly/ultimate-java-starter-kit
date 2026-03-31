package com.lunisoft.ultimatejavastarterkit.module.demo.dto;

import java.util.UUID;

public record DemoSearchCustomerResponse(
    UUID id,
    String email,
    String countryCode,
    String role
) {}
