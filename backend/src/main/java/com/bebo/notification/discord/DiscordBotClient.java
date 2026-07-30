package com.bebo.notification.discord;

import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
@ConditionalOnProperty(prefix = "bebo.discord", name = "enabled", havingValue = "true")
public class DiscordBotClient {

  private final DiscordProperties properties;

  private final RestClient restClient =
      RestClient.builder().baseUrl("https://discord.com/api/v10").build();

  public DiscordBotClient(DiscordProperties properties) {
    this.properties = properties;
  }

  public void sendDirectMessage(String discordUserId, String messageBody) {
    String directMessageChannelId = openDirectMessage(discordUserId);

    sendChannelMessage(directMessageChannelId, messageBody);
  }

  private String openDirectMessage(String discordUserId) {
    JsonNode response =
        restClient
            .post()
            .uri("/users/@me/channels")
            .header(HttpHeaders.AUTHORIZATION, botAuthorization())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("recipient_id", requireText(discordUserId, "Discord user ID")))
            .retrieve()
            .body(JsonNode.class);

    if (response == null) {
      throw new IllegalStateException("Discord create DM response was empty");
    }

    String channelId = response.path("id").asString();

    if (channelId.isBlank()) {
      throw new IllegalStateException("Discord create DM response did not contain a channel ID");
    }

    return channelId;
  }

  private void sendChannelMessage(String channelId, String messageBody) {
    restClient
        .post()
        .uri("/channels/{channelId}/messages", channelId)
        .header(HttpHeaders.AUTHORIZATION, botAuthorization())
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "content",
                requireText(messageBody, "Discord message body"),
                "allowed_mentions",
                Map.of("parse", List.of())))
        .retrieve()
        .toBodilessEntity();
  }

  private String botAuthorization() {
    return "Bot " + requireText(properties.getBotToken(), "Discord bot token");
  }

  private String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }
}
