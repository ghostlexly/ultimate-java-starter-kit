package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram")
public record TelegramProperties(String token) {}
