package com.lunisoft.javastarter.module.auth.cron;

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
public class ClearExpiredSessionsCron {

    private final Logger log = LoggerFactory.getLogger(ClearExpiredSessionsCron.class);

    private final SessionRepository sessionRepository;

    @Scheduled(fixedDelayString = "PT1H") // every hour
    @Transactional
    public void execute() {
        log.info("[⏰ CRON] Clear expired sessions cron started");

        try {
            sessionRepository.deleteByExpiresAtBefore(Instant.now());
        } catch (RuntimeException e) {
            log.error("Error during expired sessions clearing", e);
        } finally {
            log.info("[⏰ CRON] Clear expired sessions cron finished");
        }
    }
}
