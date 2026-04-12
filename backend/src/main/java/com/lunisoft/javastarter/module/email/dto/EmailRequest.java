package com.lunisoft.javastarter.module.email.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable transactional email request. Use {@link Builder} to construct.
 *
 * <pre>{@code
 * var request = EmailRequest.builder()
 *         .to("client@example.com", "Jean Dupont")
 *         .templateId(42)
 *         .subject("Votre commande")
 *         .params(Map.of("orderNumber", "CMD-001"))
 *         .attachment("facture.pdf", base64Content)
 *         .build();
 * }</pre>
 */
public record EmailRequest(
    String recipientEmail,
    String recipientName,
    long templateId,
    String subject,
    Map<String, Object> params,
    List<Map<String, String>> attachments) {

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private String recipientEmail;
    private String recipientName;
    private long templateId;
    private String subject;
    private Map<String, Object> params;
    private final List<Map<String, String>> attachments = new ArrayList<>();

    public Builder to(String email) {
      this.recipientEmail = email;

      return this;
    }

    public Builder to(String email, String name) {
      this.recipientEmail = email;
      this.recipientName = name;

      return this;
    }

    public Builder templateId(long templateId) {
      this.templateId = templateId;

      return this;
    }

    public Builder subject(String subject) {
      this.subject = subject;

      return this;
    }

    public Builder params(Map<String, Object> params) {
      this.params = params;

      return this;
    }

    /**
     * Attach a file to the email.
     *
     * @param name filename (e.g. "facture.pdf")
     * @param content Base64-encoded file content
     */
    public Builder attachment(String name, String content) {
      this.attachments.add(Map.of("name", name, "content", content));

      return this;
    }

    public EmailRequest build() {
      return new EmailRequest(
          recipientEmail, recipientName, templateId, subject, params, List.copyOf(attachments));
    }
  }
}
