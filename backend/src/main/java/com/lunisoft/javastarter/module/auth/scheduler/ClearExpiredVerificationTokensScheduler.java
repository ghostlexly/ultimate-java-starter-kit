package com.lunisoft.javastarter.module.auth.scheduler;

import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Hourly cleanup of verification tokens whose expiry has passed.
 */
@Component
@RequiredArgsConstructor
public class ClearExpiredVerificationTokensScheduler {

    private final Logger log = LoggerFactory.getLogger(ClearExpiredVerificationTokensScheduler.class);

    private final VerificationTokenRepository verificationTokenRepository;

    @Scheduled(fixedDelayString = "PT1H") // every hour
    @Transactional
    public void execute() {
        log.info("[⏰ SCHEDULER] Started ✅");

        try {
            long clearedVerificationTokensCount = verificationTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

            log.info("[⏰ SCHEDULER] Removed {} verification tokens", clearedVerificationTokensCount);
        } catch (RuntimeException e) {
            log.error("[⏰ SCHEDULER] Error ❌ : {}", e.getMessage(), e);
        } finally {
            log.info("[⏰ SCHEDULER] Finished ✅");
        }
    }
}
