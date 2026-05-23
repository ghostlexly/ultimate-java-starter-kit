package com.lunisoft.javastarter.module.email.service;

import com.lunisoft.javastarter.module.email.dto.EmailAttachment;
import com.lunisoft.javastarter.module.email.dto.EmailRequest;
import com.lunisoft.javastarter.module.email.exception.EmailSendException;
import com.lunisoft.javastarter.property.BrevoProperties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Brevo implementation of {@link EmailService}. Sends transactional emails via the Brevo HTTP API
 * using template IDs and dynamic template parameters.
 *
 * @see <a href="https://developers.brevo.com/reference/sendtransacemail">Brevo API docs</a>
 */
@Service
@Primary
public class BrevoEmailService implements EmailService {

  private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);
  private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

  private final RestClient restClient;
  private final BrevoProperties brevoProperties;

  public BrevoEmailService(BrevoProperties brevoProperties) {
    this.brevoProperties = brevoProperties;
    this.restClient =
        RestClient.builder()
            .baseUrl(BREVO_API_URL)
            .defaultHeader("api-key", brevoProperties.apiKey())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Override
  @Retryable(backoff = @Backoff(delay = 2000), maxAttempts = 10)
  public void send(EmailRequest request) {
    var body = buildBody(request);

    try {
      restClient
          .post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .toBodilessEntity();

      log.info(
          "Brevo email sent — subject={}, template={}, to={}, params={}",
          request.subject(),
          request.templateId(),
          request.recipientEmail(),
          request.params());
    } catch (Exception e) {
      log.error(
          "Failed to send Brevo email — subject={}, template={}, to={}, params={}",
          request.subject(),
          request.templateId(),
          request.recipientEmail(),
          request.params(),
          e);

      throw new EmailSendException(
          "Failed to send Brevo email — template=%d, to=%s"
              .formatted(request.templateId(), request.recipientEmail()),
          e);
    }
  }

  private Map<String, Object> buildBody(EmailRequest request) {
    var sender =
        Map.of(
            "name", brevoProperties.senderName(),
            "email", brevoProperties.senderEmail());

    Map<String, String> recipient =
        request.recipientName() != null
            ? Map.of("email", request.recipientEmail(), "name", request.recipientName())
            : Map.of("email", request.recipientEmail());

    var body = new HashMap<String, Object>();
    body.put("sender", sender);
    body.put("to", List.of(recipient));
    body.put("templateId", request.templateId());

    if (request.subject() != null) {
      body.put("subject", request.subject());
    }

    if (request.params() != null && !request.params().isEmpty()) {
      body.put("params", request.params());
    }

    if (request.attachments() != null && !request.attachments().isEmpty()) {
      body.put("attachment", toBrevoAttachments(request.attachments()));
    }

    return body;
  }

  /** Brevo expects attachments as a list of {@code {"name": ..., "content": ...}} objects. */
  private List<Map<String, String>> toBrevoAttachments(List<EmailAttachment> attachments) {
    return attachments.stream()
        .map(att -> Map.of("name", att.name(), "content", att.content()))
        .toList();
  }
}
