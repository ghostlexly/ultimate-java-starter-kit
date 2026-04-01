package com.lunisoft.ultimatejavastarterkit.module.auth.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for login code events and sends the code via email. TODO: Replace log with actual email
 * service (Brevo, SendGrid, etc.).
 */
@Component
public class LoginCodeRequestedListener {
  private static final Logger log = LoggerFactory.getLogger(LoginCodeRequestedListener.class);

  @Async
  @EventListener
  public void handle(LoginCodeRequestedEvent event) {
    log.info("Login code for {}: {}", event.email(), event.code());
  }
}
