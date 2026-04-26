package com.lunisoft.javastarter.module.auth.usecase.verifycode;

import jakarta.servlet.http.HttpServletRequest;

public record VerifyCodeInput(String email, String code, HttpServletRequest request) {}
