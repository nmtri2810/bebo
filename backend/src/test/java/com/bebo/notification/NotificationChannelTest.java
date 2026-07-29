package com.bebo.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.bebo.user.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NotificationChannelTest {

  private final User user = User.create("test@example.com", "{noop}secret", "UTC");

  @Test
  void beginConnectionMarksTelegramChannelPending() {
    NotificationChannel channel = NotificationChannel.telegram(user);

    Instant expiresAt = Instant.parse("2026-07-29T08:00:00Z");

    channel.beginConnection("token-hash", expiresAt);

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.PENDING);
    assertThat(channel.getConnectTokenHash()).isEqualTo("token-hash");
    assertThat(channel.getConnectTokenExpiresAt()).isEqualTo(expiresAt);
    assertThat(channel.getTelegramChatId()).isNull();
    assertThat(channel.getTelegramUsername()).isNull();
  }

  @Test
  void connectTelegramMarksConnectedAndClearsPendingToken() {
    NotificationChannel channel = NotificationChannel.telegram(user);

    channel.beginConnection("token-hash", Instant.parse("2026-07-29T08:00:00Z"));

    Instant connectedAt = Instant.parse("2026-07-29T07:59:00Z");

    channel.connectTelegram(123456789L, "bebo_user", connectedAt);

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.CONNECTED);
    assertThat(channel.getTelegramChatId()).isEqualTo(123456789L);
    assertThat(channel.getTelegramUsername()).isEqualTo("bebo_user");
    assertThat(channel.getExternalRecipientId()).isEqualTo("123456789");
    assertThat(channel.getConnectedAt()).isEqualTo(connectedAt);
    assertThat(channel.getConnectTokenHash()).isNull();
    assertThat(channel.getConnectTokenExpiresAt()).isNull();
  }

  @Test
  void disconnectClearsTelegramConnectionDetails() {
    NotificationChannel channel = NotificationChannel.telegram(user);

    channel.beginConnection("token-hash", Instant.parse("2026-07-29T08:00:00Z"));
    channel.connectTelegram(123456789L, "bebo_user", Instant.parse("2026-07-29T07:59:00Z"));

    channel.disconnect();

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.DISCONNECTED);
    assertThat(channel.getTelegramChatId()).isNull();
    assertThat(channel.getTelegramUsername()).isNull();
    assertThat(channel.getConnectedAt()).isNull();
    assertThat(channel.getConnectTokenHash()).isNull();
    assertThat(channel.getConnectTokenExpiresAt()).isNull();
  }
}
