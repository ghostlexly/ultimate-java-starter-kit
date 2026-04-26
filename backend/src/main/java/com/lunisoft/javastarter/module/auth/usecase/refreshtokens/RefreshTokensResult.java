package com.lunisoft.javastarter.module.auth.usecase.refreshtokens;

public record RefreshTokensResult(String role, String accessToken, String refreshToken) {}
