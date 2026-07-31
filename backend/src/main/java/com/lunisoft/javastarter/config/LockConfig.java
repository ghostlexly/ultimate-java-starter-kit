package com.lunisoft.javastarter.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson client backing the distributed locks (and semaphores) of the analysis flows. Locks
 * acquired WITHOUT an explicit lease benefit from the Redisson watchdog: the lease is renewed
 * automatically while the holder is alive, and expires if its JVM dies (crash safety net).
 */
@Configuration
public class LockConfig {

    /**
     * Built from Spring Boot's connection details so it points at the same Redis as Spring Data —
     * including the Testcontainers instance during integration tests (@ServiceConnection).
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(DataRedisConnectionDetails connectionDetails) {
        var standalone = connectionDetails.getStandalone();

        // getStandalone() is null when Redis is configured as sentinel/cluster — unsupported here
        if (standalone == null) {
            throw new IllegalStateException(
                    "Redis must be configured in standalone mode for the Redisson lock client.");
        }

        var config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress("redis://%s:%d".formatted(standalone.getHost(), standalone.getPort()))
                .setDatabase(standalone.getDatabase());

        if (StringUtils.hasText(connectionDetails.getUsername())) {
            serverConfig.setUsername(connectionDetails.getUsername());
        }

        if (StringUtils.hasText(connectionDetails.getPassword())) {
            serverConfig.setPassword(connectionDetails.getPassword());
        }

        return Redisson.create(config);
    }
}
