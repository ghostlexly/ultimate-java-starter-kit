package com.lunisoft.ultimatejavastarterkit.module.auth.dto;

import java.util.UUID;

public record MeResponse(
    UUID accountId,
    String email,
    String role
) {}
