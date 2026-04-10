package com.lunisoft.javastarter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.pdf")
public record PdfProperties(int renderTimeoutMs, String format, boolean printBackground) {}
