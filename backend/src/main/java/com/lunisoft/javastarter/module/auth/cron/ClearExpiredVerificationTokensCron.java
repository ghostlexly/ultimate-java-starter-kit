package com.lunisoft.javastarter.module.auth.cron;

import com.lunisoft.javastarter.module.auth.repository.VerificationTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hourly cleanup of verification tokens whose expiry has passed.
 */
@Component
@RequiredArgsConstructor
public class ClearExpiredVerificationTokensCron {

  private final Logger log = LoggerFactory.getLogger(ClearExpiredVerificationTokensCron.class);

  private final VerificationTokenRepository verificationTokenRepository;

  @Scheduled(fixedDelayString = "PT1H") // every hour
  @Transactional
  public void execute() {
    log.info("[⏰ CRON] Clear expired verification tokens cron started");

    try {
      verificationTokenRepository.deleteByExpiresAtBefore(Instant.now());
    } catch (RuntimeException e) {
      log.error("Error during expired verification tokens clearing", e);
    } finally {
      log.info("[⏰ CRON] Clear expired verification tokens cron finished");
    }
  }
}
