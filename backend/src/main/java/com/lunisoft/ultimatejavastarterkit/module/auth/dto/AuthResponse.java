package com.lunisoft.ultimatejavastarterkit.module.auth.dto;

public record AuthResponse(
    String role,
    String accessToken,
    String refreshToken
) {}
