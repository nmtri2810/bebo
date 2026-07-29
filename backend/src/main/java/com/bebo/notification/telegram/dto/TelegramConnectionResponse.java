package com.bebo.notification.telegram.dto;

import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;

public record TelegramConnectionResponse(
    NotificationChannelStatus status,
    boolean connected,
    String telegramUsername,
    Instant connectedAt) {

  public static TelegramConnectionResponse disconnected() {
    return new TelegramConnectionResponse(
        NotificationChannelStatus.DISCONNECTED, false, null, null);
  }

  public static TelegramConnectionResponse from(NotificationChannel channel) {
    return new TelegramConnectionResponse(
        channel.getConnectionStatus(),
        channel.getConnectionStatus() == NotificationChannelStatus.CONNECTED,
        channel.getTelegramUsername(),
        channel.getConnectedAt());
  }
}
