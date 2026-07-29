package com.bebo.notification.telegram;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.telegram")
public class TelegramProperties {

  private boolean enabled;

  private String botToken = "";

  private String botUsername = "";

  private Duration connectionTokenTtl = Duration.ofMinutes(10);

  private long pollDelayMs = 3000;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBotToken() {
    return botToken;
  }

  public void setBotToken(String botToken) {
    this.botToken = botToken;
  }

  public String getBotUsername() {
    return botUsername;
  }

  public void setBotUsername(String botUsername) {
    this.botUsername = botUsername;
  }

  public Duration getConnectionTokenTtl() {
    return connectionTokenTtl;
  }

  public void setConnectionTokenTtl(Duration connectionTokenTtl) {
    this.connectionTokenTtl = connectionTokenTtl;
  }

  public long getPollDelayMs() {
    return pollDelayMs;
  }

  public void setPollDelayMs(long pollDelayMs) {
    this.pollDelayMs = pollDelayMs;
  }
}
