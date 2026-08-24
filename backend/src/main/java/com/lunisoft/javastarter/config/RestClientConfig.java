package com.lunisoft.javastarter.config;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofMinutes(2);

    @Bean
    @Primary
    public RestClient restClient(
            RestClient.Builder builder,
            ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
            HttpClientSettings clientSettings) {

        HttpClientSettings settings = clientSettings.withTimeouts(CONNECT_TIMEOUT, READ_TIMEOUT);

        return builder.requestFactory(requestFactoryBuilder.build(settings)).build();
    }
}
