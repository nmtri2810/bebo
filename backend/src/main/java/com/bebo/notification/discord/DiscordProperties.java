package com.bebo.notification.discord;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.discord")
public class DiscordProperties {

  private boolean enabled;

  private String clientId = "";

  private String clientSecret = "";

  private String botToken = "";

  private String redirectUri =
      "http://localhost:8080/api/public/notification-channels/discord/callback";

  private String frontendRedirectUrl = "http://localhost:3000/settings";

  private Duration stateTtl = Duration.ofMinutes(10);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getBotToken() {
    return botToken;
  }

  public void setBotToken(String botToken) {
    this.botToken = botToken;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getFrontendRedirectUrl() {
    return frontendRedirectUrl;
  }

  public void setFrontendRedirectUrl(String frontendRedirectUrl) {
    this.frontendRedirectUrl = frontendRedirectUrl;
  }

  public Duration getStateTtl() {
    return stateTtl;
  }

  public void setStateTtl(Duration stateTtl) {
    this.stateTtl = stateTtl;
  }
}
