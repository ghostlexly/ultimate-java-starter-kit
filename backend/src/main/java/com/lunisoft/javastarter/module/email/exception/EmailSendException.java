package com.lunisoft.javastarter.module.email.exception;

/**
 * Thrown by {@code EmailService} implementations when the underlying provider call fails. The
 * message carries enough context (template id, recipient) to diagnose the failure; the original
 * provider exception is preserved as the cause.
 */
public class EmailSendException extends RuntimeException {

  public EmailSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
