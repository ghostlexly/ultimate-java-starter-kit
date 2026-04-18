package com.lunisoft.javastarter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String privateKey,
    String publicKey,
    int accessTokenExpirationMinutes,
    int refreshTokenExpirationMinutes) {}
