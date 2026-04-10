package com.lunisoft.javastarter.module.customer.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchronous listener that reacts to email changes within the same transaction. If this listener
 * throws, the entire transaction (including the email update) rolls back.
 */
@Component
public class CustomerEmailUpdatedListener {

  private static final Logger log = LoggerFactory.getLogger(CustomerEmailUpdatedListener.class);

  @EventListener
  public void handle(CustomerEmailUpdatedEvent event) {
    detectCountryFromEmail(event);
  }

  /** Auto-detects country code based on known email addresses. */
  private void detectCountryFromEmail(CustomerEmailUpdatedEvent event) {
    String newEmail = event.newEmail();

    if ("contact@lunisoft.fr".equalsIgnoreCase(newEmail)) {
      //      event.customer().setCountryCode("FR");
      log.info("Auto-detected country code FR for email {}", newEmail);
    }
  }
}
