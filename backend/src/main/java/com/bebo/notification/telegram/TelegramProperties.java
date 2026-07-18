package com.bebo.notification.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bebo.telegram")
public record TelegramProperties(String botToken, String botUsername, String webhookSecret) {
  public boolean isConfigured() {
    return botToken != null && !botToken.isBlank();
  }
}
