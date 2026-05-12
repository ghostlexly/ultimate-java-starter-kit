package com.lunisoft.javastarter.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Top-level email configuration. The {@code provider} field selects which {@code EmailService}
 * implementation is wired at runtime (e.g. {@code "brevo"}, {@code "sendgrid"}, ...). Each provider
 * also has its own dedicated properties (e.g. {@link BrevoProperties}).
 *
 * <pre>{@code
 * app:
 *   email:
 *     provider: brevo
 *     brevo:
 *       api-key: ...
 * }</pre>
 */
@ConfigurationProperties(prefix = "app.email")
public record EmailProperties(String provider) {}
