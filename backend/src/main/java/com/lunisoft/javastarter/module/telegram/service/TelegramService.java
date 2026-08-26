package com.lunisoft.javastarter.module.telegram.service;

import com.lunisoft.javastarter.module.telegram.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);
    private final TelegramClient telegramClient;
    private final Environment environment;

    /**
     * To find the chatId, send a message to the bot from your phone and then call this request : GET https://api.telegram.org/botTON_BOT_TOKEN/getUpdates
     */
    @Async
    public void sendMessage(String message) {
        // If in Dev profile, do not send a telegram message
        if (environment.matchesProfiles("dev")) {
            return;
        }

        var response = telegramClient
                .getRestClient()
                .post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessageRequest("1904642890", message))
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            log.error("Failed to send message to Telegram. Response is null.");
            throw new IllegalArgumentException("Failed to send message to Telegram");
        }

        if (!response.has("ok") || !response.get("ok").asBoolean()) {
            log.error("Failed to send message to Telegram. Response is not ok. \n Response: {}", response);
            throw new IllegalArgumentException("Failed to send message to Telegram");
        }
    }
}
