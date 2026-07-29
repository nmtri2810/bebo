package com.bebo.notification.telegram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class TelegramBotClient {

  private final TelegramProperties properties;

  private final RestClient restClient =
      RestClient.builder().baseUrl("https://api.telegram.org").build();

  public TelegramBotClient(TelegramProperties properties) {
    this.properties = properties;
  }

  public List<TelegramIncomingMessage> getUpdates(long offset) {
    JsonNode response =
        restClient
            .post()
            .uri("/bot" + properties.getBotToken() + "/getUpdates")
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "offset",
                    offset,
                    "limit",
                    100,
                    "timeout",
                    0,
                    "allowed_updates",
                    List.of("message")))
            .retrieve()
            .body(JsonNode.class);

    if (response == null || !response.path("ok").asBoolean()) {
      throw new IllegalStateException("Telegram getUpdates failed");
    }

    List<TelegramIncomingMessage> messages = new ArrayList<>();

    for (JsonNode update : response.path("result")) {
      long updateId = update.path("update_id").asLong();

      JsonNode message = update.path("message");

      if (message.isMissingNode() || message.isNull()) {
        messages.add(new TelegramIncomingMessage(updateId, 0, null, null));

        continue;
      }

      long chatId = message.path("chat").path("id").asLong();

      String text = nullableText(message.path("text"));

      String username = nullableText(message.path("from").path("username"));

      messages.add(new TelegramIncomingMessage(updateId, chatId, username, text));
    }

    return messages;
  }

  public void sendMessage(long chatId, String text) {
    restClient
        .post()
        .uri("/bot" + properties.getBotToken() + "/sendMessage")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("chat_id", chatId, "text", text))
        .retrieve()
        .toBodilessEntity();
  }

  private String nullableText(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return null;
    }

    return node.asString();
  }

  public record TelegramIncomingMessage(long updateId, long chatId, String username, String text) {}
}
