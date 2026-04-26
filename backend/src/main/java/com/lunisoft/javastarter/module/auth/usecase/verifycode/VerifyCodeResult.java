package com.lunisoft.javastarter.module.auth.usecase.verifycode;

public record VerifyCodeResult(String role, String accessToken, String refreshToken) {}
