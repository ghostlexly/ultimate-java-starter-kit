package com.lunisoft.ultimatejavastarterkit.module.customer.dto;

import java.util.UUID;

public record CustomerResponse(UUID id, String email, String countryCode) {}
