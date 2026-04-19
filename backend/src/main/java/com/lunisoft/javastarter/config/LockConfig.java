package com.lunisoft.javastarter.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

@Configuration
public class LockConfig {

  @Bean
  public RedisLockRegistry lockRegistry(RedisConnectionFactory connectionFactory) {
    // RegistryKey "locks" = préfixe des clés Redis
    // 60_000L = expiration du verrou en ms (safety net si le process crash)
    return new RedisLockRegistry(connectionFactory, "locks", Duration.ofSeconds(60).toMillis());
  }
}
