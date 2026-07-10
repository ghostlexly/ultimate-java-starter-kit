package com.lunisoft.javastarter.module.email.service;

import com.lunisoft.javastarter.module.email.dto.EmailRequest;

/**
 * Provider-agnostic transactional email sender.
 *
 * <p>Inject this interface — never a concrete implementation — so the underlying provider (Brevo,
 * SendGrid, Mailjet, ...) can be swapped via configuration without touching call sites.
 *
 * <p>Provider selection is driven by the {@code app.email.provider} property. A single
 * implementation is active at runtime, picked through {@code @ConditionalOnProperty}.
 */
public interface EmailService {

    /**
     * Send a transactional email built from a logical {@link EmailRequest}. Implementations are
     * responsible for translating the request into the provider's wire format and applying any retry
     * / error-handling policy.
     *
     * @param request the email to send (recipient, template id, params, attachments, ...)
     */
    void send(EmailRequest request);
}
