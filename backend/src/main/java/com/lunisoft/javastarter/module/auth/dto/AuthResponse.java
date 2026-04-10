package com.lunisoft.javastarter.module.auth.dto;

public record AuthResponse(String role, String accessToken, String refreshToken) {}
