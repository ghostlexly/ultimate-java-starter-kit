package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.brevo")
public record BrevoProperties(String apiKey, String senderName, String senderEmail) {}
