package com.bebo.notification.discord.dto;

import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;

public record DiscordConnectionResponse(
    NotificationChannelStatus status,
    boolean connected,
    String discordUsername,
    Instant connectedAt) {

  public static DiscordConnectionResponse from(NotificationChannel channel) {
    boolean connected =
        channel.isEnabled()
            && channel.getConnectionStatus() == NotificationChannelStatus.CONNECTED
            && channel.getExternalRecipientId() != null;

    return new DiscordConnectionResponse(
        channel.getConnectionStatus(),
        connected,
        channel.getExternalUsername(),
        channel.getConnectedAt());
  }

  public static DiscordConnectionResponse disconnected() {
    return new DiscordConnectionResponse(NotificationChannelStatus.DISCONNECTED, false, null, null);
  }
}
