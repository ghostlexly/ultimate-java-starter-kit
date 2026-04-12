package com.lunisoft.javastarter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.brevo")
public record BrevoProperties(String apiKey, String senderName, String senderEmail) {}
