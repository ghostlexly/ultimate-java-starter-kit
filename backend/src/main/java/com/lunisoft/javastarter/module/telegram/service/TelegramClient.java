package com.lunisoft.javastarter.module.telegram.service;

import com.lunisoft.javastarter.property.TelegramProperties;
import lombok.Getter;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class TelegramClient {
    private static final String TELEGRAM_BASE_URL = "https://api.telegram.org";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Getter
    private final RestClient restClient;

    public TelegramClient(TelegramProperties telegramProperties) {
        this.restClient = RestClient.builder()
                .baseUrl("%s/bot%s".formatted(TELEGRAM_BASE_URL, telegramProperties.token()))
                .requestFactory(buildRequestFactory())
                .build();
    }

    private static ClientHttpRequestFactory buildRequestFactory() {
        HttpClientSettings settings = HttpClientSettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);

        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }
}
