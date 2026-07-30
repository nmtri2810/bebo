package com.bebo.notification.discord;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
@ConditionalOnProperty(prefix = "bebo.discord", name = "enabled", havingValue = "true")
public class DiscordOAuthClient {

  private final DiscordProperties properties;

  private final RestClient restClient =
      RestClient.builder().baseUrl("https://discord.com/api/v10").build();

  public DiscordOAuthClient(DiscordProperties properties) {
    this.properties = properties;
  }

  public String exchangeAuthorizationCode(String authorizationCode) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

    form.add("client_id", properties.getClientId());

    form.add("client_secret", properties.getClientSecret());

    form.add("grant_type", "authorization_code");

    form.add("code", requireText(authorizationCode, "Discord authorization code"));

    form.add("redirect_uri", properties.getRedirectUri());

    JsonNode response =
        restClient
            .post()
            .uri("/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(JsonNode.class);

    if (response == null) {
      throw new IllegalStateException("Discord token response was empty");
    }

    return requiredJsonText(response, "access_token");
  }

  public DiscordUser getCurrentUser(String accessToken) {
    JsonNode response =
        restClient
            .get()
            .uri("/users/@me")
            .header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + requireText(accessToken, "Discord access token"))
            .retrieve()
            .body(JsonNode.class);

    if (response == null) {
      throw new IllegalStateException("Discord user response was empty");
    }

    String id = requiredJsonText(response, "id");

    String username = requiredJsonText(response, "username");

    String globalName = nullableJsonText(response, "global_name");

    return new DiscordUser(id, username, globalName);
  }

  private String requiredJsonText(JsonNode response, String fieldName) {
    String value = nullableJsonText(response, fieldName);

    if (value == null) {
      throw new IllegalStateException("Discord response did not contain " + fieldName);
    }

    return value;
  }

  private String nullableJsonText(JsonNode response, String fieldName) {
    JsonNode node = response.path(fieldName);

    if (node.isMissingNode() || node.isNull()) {
      return null;
    }

    String value = node.asString();

    if (value.isBlank()) {
      return null;
    }

    return value;
  }

  private String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }

    return value.trim();
  }

  public record DiscordUser(String id, String username, String globalName) {

    public String displayName() {
      if (globalName != null && !globalName.isBlank()) {
        return globalName;
      }

      return username;
    }
  }
}
