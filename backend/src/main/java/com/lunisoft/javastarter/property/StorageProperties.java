package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
    String endpoint, String region, String accessKey, String secretKey, String bucket) {}
