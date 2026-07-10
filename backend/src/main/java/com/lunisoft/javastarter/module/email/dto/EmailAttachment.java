package com.lunisoft.javastarter.module.email.dto;

import org.springframework.util.StringUtils;

/**
 * Provider-agnostic email attachment.
 *
 * @param name    filename shown to the recipient (e.g. {@code "invoice.pdf"})
 * @param content Base64-encoded file content
 */
public record EmailAttachment(String name, String content) {

    public EmailAttachment {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("attachment name cannot be blank");
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("attachment content cannot be blank");
        }
    }
}
