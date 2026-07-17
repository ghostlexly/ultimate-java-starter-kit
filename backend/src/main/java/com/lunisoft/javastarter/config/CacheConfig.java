package com.lunisoft.javastarter.config;

import com.lunisoft.javastarter.module.demo.usecase.GetCachedTimeUseCase;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String DEMO_CACHED_TIME = "DEMO_CACHED_TIME";
    public static final String S3_PRESIGNED_GET_URL = "S3_PRESIGNED_GET_URL";

    /**
     * Fallback TTL for caches not registered in {@link #cacheConfigurations()}.
     */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigurations())
                .build();
    }

    /**
     * Central registry of every cache: name -> value type + TTL. Register each new cache here so it
     * gets an explicit TTL and a typed serializer (no type metadata stored in Redis).
     */
    private Map<String, RedisCacheConfiguration> cacheConfigurations() {
        return Map.of(
                DEMO_CACHED_TIME,
                cacheConfiguration(GetCachedTimeUseCase.Output.class, Duration.ofSeconds(10)),
                S3_PRESIGNED_GET_URL,
                cacheConfiguration(String.class, Duration.ofHours(4)));
    }

    /**
     * Never let a Redis outage take the application down: cache errors are logged and the call falls
     * through to the underlying method (i.e. the database).
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    /**
     * Cache config bound to a specific value type.
     */
    private RedisCacheConfiguration cacheConfiguration(Class<?> type, Duration ttl) {
        return baseCacheConfiguration(ttl, new JacksonJsonRedisSerializer<>(type));
    }

    /**
     * Defaults for caches used without being registered above: short TTL + generic JSON serializer
     * (stores {@code @class} metadata so values can be deserialized without a known type).
     */
    private RedisCacheConfiguration defaultCacheConfiguration() {
        return baseCacheConfiguration(
                DEFAULT_TTL, GenericJacksonJsonRedisSerializer.builder().build());
    }

    /**
     * Common settings shared by every cache: string keys, JSON values, no null caching.
     */
    private RedisCacheConfiguration baseCacheConfiguration(Duration ttl, RedisSerializer<?> valueSerializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(valueSerializer));
    }
}
