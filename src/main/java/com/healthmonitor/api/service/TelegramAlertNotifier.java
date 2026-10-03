package com.healthmonitor.api.service;

import com.healthmonitor.api.model.AlertType;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends triggered alerts to a Telegram chat. Active only when both
 * TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID are configured; otherwise it is a
 * silent no-op so the API never fails because of a missing integration.
 * Delivery runs off the request thread and failures are logged, never thrown.
 */
@Component
public class TelegramAlertNotifier implements AlertNotifier {

    private static final Logger log = Logger.getLogger(TelegramAlertNotifier.class.getName());

    private final RestClient restClient;
    private final String botToken;
    private final String chatId;

    public TelegramAlertNotifier(
        @Value("${healthmonitor.telegram.bot-token}") String botToken,
        @Value("${healthmonitor.telegram.chat-id}") String chatId
    ) {
        this.botToken = botToken == null ? "" : botToken.trim();
        this.chatId = chatId == null ? "" : chatId.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
            .baseUrl("https://api.telegram.org")
            .requestFactory(factory)
            .build();
    }

    public boolean isActive() {
        return !botToken.isBlank() && !chatId.isBlank();
    }

    @Override
    @Async
    public void send(Long patientId, AlertType type, String message) {
        if (!isActive()) {
            return;
        }
        try {
            String text = "Health Monitor Alert — patient #" + patientId + "\n"
                + type + ": " + message;
            restClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.log(Level.WARNING, "Telegram notification failed: {0}", e.getMessage());
        }
    }
}
