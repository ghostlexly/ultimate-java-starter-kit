package com.lunisoft.javastarter.module.email.service;

import com.lunisoft.javastarter.config.BrevoProperties;
import com.lunisoft.javastarter.module.email.dto.EmailRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Sends transactional emails via the Brevo HTTP API. Uses template IDs and dynamic template
 * parameters.
 *
 * @see <a href="https://developers.brevo.com/reference/sendtransacemail">Brevo API docs</a>
 */
@Service
public class BrevoEmailService {

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

  /**
   * Send a transactional email using a Brevo template.
   *
   * <pre>{@code
   * var request = new EmailRequest()
   *         .to("client@example.com", "Jean Dupont")
   *         .templateId(42)
   *         .subject("Votre code")
   *         .params(Map.of("code", "123456"))
   *         .attachment("facture.pdf", base64Content);
   *
   * brevoEmailService.send(request);
   * }</pre>
   */
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
          "Brevo email sent — template={}, to={}", request.templateId(), request.recipientEmail());
    } catch (Exception e) {
      log.error(
          "Failed to send Brevo email — template={}, to={}: {}",
          request.templateId(),
          request.recipientEmail(),
          e.getMessage());
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
      body.put("attachment", request.attachments());
    }

    return body;
  }
}
