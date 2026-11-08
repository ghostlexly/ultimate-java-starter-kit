package com.lunisoft.javastarter.module.auth.scheduler;

import com.lunisoft.javastarter.module.auth.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Hourly cleanup of sessions whose expiry has passed.
 */
@Component
@RequiredArgsConstructor
public class ClearExpiredSessionsScheduler {

    private final Logger log = LoggerFactory.getLogger(ClearExpiredSessionsScheduler.class);

    private final SessionRepository sessionRepository;

    @Scheduled(fixedDelayString = "PT1H") // every hour
    @Transactional
    public void execute() {
        log.info("[⏰ SCHEDULER] Started ✅");

        try {
            long clearedSessionsCount = sessionRepository.deleteAllByExpiresAtBefore(Instant.now());

            log.info("[⏰ SCHEDULER] Removed {} sessions", clearedSessionsCount);
        } catch (RuntimeException e) {
            log.error("[⏰ SCHEDULER] Error ❌ : {}", e.getMessage(), e);
        } finally {
            log.info("[⏰ SCHEDULER] Finished ✅");
        }
    }
}
