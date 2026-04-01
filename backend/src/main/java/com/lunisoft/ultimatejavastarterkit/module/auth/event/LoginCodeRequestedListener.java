package com.lunisoft.ultimatejavastarterkit.module.auth.event;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for login code events and sends the code via email. TODO: Replace log with actual email
 * service (Brevo, SendGrid, etc.).
 */
@Slf4j
@Component
public class LoginCodeRequestedListener {

  @Async
  @EventListener
  public void handle(LoginCodeRequestedEvent event) {
    log.info("Login code for {}: {}", event.email(), event.code());
  }
}
