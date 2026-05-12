package com.lunisoft.javastarter.module.email.dto;

import java.util.Objects;

/**
 * Provider-agnostic email attachment.
 *
 * @param name filename shown to the recipient (e.g. {@code "invoice.pdf"})
 * @param content Base64-encoded file content
 */
public record EmailAttachment(String name, String content) {

  public EmailAttachment {
    Objects.requireNonNull(name, "attachment name is required");
    Objects.requireNonNull(content, "attachment content is required");

    if (name.isBlank()) {
      throw new IllegalArgumentException("attachment name cannot be blank");
    }

    if (content.isBlank()) {
      throw new IllegalArgumentException("attachment content cannot be blank");
    }
  }
}
