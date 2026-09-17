package com.lunisoft.javastarter.config;

import com.lunisoft.javastarter.property.S3Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());

        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .build();
    }

    /**
     * Async client dedicated to uploads: with multipart enabled, it can stream content of unknown
     * length (see {@code S3Service#upload}), which the sync client cannot do without buffering it all.
     */
    @Bean
    public S3AsyncClient s3AsyncClient(S3Properties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());

        return S3AsyncClient.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true)
                .multipartEnabled(true)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties) {
        var credentials = AwsBasicCredentials.create(properties.accessKey(), properties.secretKey());

        return S3Presigner.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
