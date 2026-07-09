package com.lunisoft.javastarter.module.email.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;


/**
 * Immutable, provider-agnostic transactional email request. Use {@link Builder} to construct.
 *
 * <pre>{@code
 * var request = EmailRequest.builder("client@example.com", 42)
 *         .subject("Votre commande")
 *         .params(Map.of("orderNumber", "CMD-001"))
 *         .attachment("facture.pdf", base64Content)
 *         .build();
 *
 * emailService.send(request);
 * }</pre>
 *
 * <p>The {@code templateId} is the numeric template identifier used by the active provider (Brevo,
 * SendGrid, Mailjet, ...). Move provider-specific values to configuration to keep call sites
 * portable.
 */
public record EmailRequest(
    String recipientEmail,
    String recipientName,
    int templateId,
    String subject,
    Map<String, Object> params,
    List<EmailAttachment> attachments
) {

  // Compact constructor = single validation point, even if someone bypasses the builder
  public EmailRequest {
    if (!StringUtils.hasText(recipientEmail)) {
      throw new IllegalArgumentException("recipientEmail is required");
    }

    if (templateId <= 0) {
      throw new IllegalArgumentException("templateId is required");
    }

    params = params == null ? Map.of() : Map.copyOf(params);
    attachments = attachments == null ? List.of() : List.copyOf(attachments);
  }

  public static Builder builder(String recipientEmail, int templateId) {
    return new Builder(recipientEmail, templateId);
  }

  public static final class Builder {

    private final String recipientEmail;
    private final int templateId;
    private String recipientName;
    private String subject;
    private final Map<String, Object> params = new HashMap<>();
    private final List<EmailAttachment> attachments = new ArrayList<>();

    private Builder(String recipientEmail, int templateId) {
      this.recipientEmail = recipientEmail;
      this.templateId = templateId;
    }

    public Builder recipientName(String recipientName) {
      this.recipientName = recipientName;
      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    public Builder param(String key, Object value) {
      this.params.put(key, value);
      return this;
    }

    public Builder params(Map<String, Object> params) {
      this.params.putAll(params);
      return this;
    }

    public Builder attachment(EmailAttachment attachment) {
      this.attachments.add(attachment);
      return this;
    }

    public EmailRequest build() {
      return new EmailRequest(recipientEmail, recipientName, templateId,
          subject, params, attachments);
    }
  }
}