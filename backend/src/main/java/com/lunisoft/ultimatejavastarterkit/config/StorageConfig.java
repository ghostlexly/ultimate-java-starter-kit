package com.lunisoft.ultimatejavastarterkit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());

        return S3Client.builder()
            .endpointOverride(URI.create(properties.endpoint()))
            .region(Region.of(properties.region()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());

        return S3Presigner.builder()
            .endpointOverride(URI.create(properties.endpoint()))
            .region(Region.of(properties.region()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}
