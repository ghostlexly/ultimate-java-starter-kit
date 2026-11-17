package com.lunisoft.javastarter.module.demo.usecase;

import com.lunisoft.javastarter.config.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class GetCachedTimeUseCase {

    public record Output(OffsetDateTime now, String formattedTime) {}

    @Cacheable(value = CacheConfig.DEMO_CACHED_TIME)
    public Output execute() {
        OffsetDateTime now = Instant.now().atOffset(ZoneOffset.UTC);
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return new Output(now, customFormatter.format(now));
    }
}
