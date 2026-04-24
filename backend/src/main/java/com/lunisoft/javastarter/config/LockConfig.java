package com.lunisoft.javastarter.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.scheduling.TaskScheduler;

@Configuration
public class LockConfig {

  @Bean
  public RedisLockRegistry lockRegistry(
      RedisConnectionFactory connectionFactory, TaskScheduler taskScheduler) {
    // RegistryKey "locks" = préfixe des clés Redis
    // 60_000L = expiration du verrou en ms (safety net si le process crash)
    RedisLockRegistry registry =
        new RedisLockRegistry(connectionFactory, "locks", Duration.ofSeconds(60).toMillis());

    // Automatically renew locks to prevent expiration until the lock.unlock() is called
    registry.setRenewalTaskScheduler(taskScheduler);

    return registry;
  }
}
