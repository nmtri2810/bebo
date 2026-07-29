package com.bebo.notification.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bebo.common.exception.BadRequestException;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.telegram.TelegramConnectionService.ConnectionAttempt;
import com.bebo.notification.telegram.dto.TelegramConnectLinkResponse;
import com.bebo.notification.telegram.dto.TelegramConnectionResponse;
import com.bebo.notification.telegram.dto.TelegramTestResponse;
import com.bebo.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TelegramConnectionServiceTest {

  @Mock private NotificationChannelRepository channelRepository;

  @Mock private TelegramConnectionTokenService tokenService;

  @Mock private ObjectProvider<TelegramBotClient> telegramBotClientProvider;

  private TelegramProperties properties;

  private TelegramConnectionService service;

  @BeforeEach
  void setUp() {
    properties = new TelegramProperties();

    properties.setEnabled(true);
    properties.setBotUsername("@bebo_cycle_bot");
    properties.setBotToken("bot-token");
    properties.setConnectionTokenTtl(Duration.ofMinutes(10));

    service =
        new TelegramConnectionService(
            channelRepository, tokenService, properties, telegramBotClientProvider);
  }

  @Test
  void getStatusReturnsDisconnectedWhenChannelDoesNotExist() {
    User user = userWithId(UUID.randomUUID());

    when(channelRepository.findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.empty());

    TelegramConnectionResponse response = service.getStatus(user);

    assertThat(response.status()).isEqualTo(NotificationChannelStatus.DISCONNECTED);

    assertThat(response.connected()).isFalse();

    assertThat(response.telegramUsername()).isNull();

    assertThat(response.connectedAt()).isNull();
  }

  @Test
  void beginConnectionCreatesPendingChannelAndDeepLink() {
    UUID userId = UUID.randomUUID();
    User user = userWithId(userId);

    when(channelRepository.findByUser_IdAndChannelType(userId, ChannelType.TELEGRAM))
        .thenReturn(Optional.empty());

    when(tokenService.issue())
        .thenReturn(new TelegramConnectionTokenService.IssuedToken("raw-token_123", "token-hash"));

    Instant before = Instant.now();

    TelegramConnectLinkResponse response = service.beginConnection(user);

    ArgumentCaptor<NotificationChannel> channelCaptor =
        ArgumentCaptor.forClass(NotificationChannel.class);

    verify(channelRepository).save(channelCaptor.capture());

    NotificationChannel savedChannel = channelCaptor.getValue();

    assertThat(savedChannel.getUser()).isSameAs(user);

    assertThat(savedChannel.getChannelType()).isEqualTo(ChannelType.TELEGRAM);

    assertThat(savedChannel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.PENDING);

    assertThat(savedChannel.getConnectTokenHash()).isEqualTo("token-hash");

    assertThat(savedChannel.getConnectTokenExpiresAt())
        .isBetween(before.plus(Duration.ofMinutes(9)), before.plus(Duration.ofMinutes(11)));

    assertThat(response.status()).isEqualTo(NotificationChannelStatus.PENDING);

    assertThat(response.deepLink()).isEqualTo("https://t.me/bebo_cycle_bot?start=raw-token_123");

    assertThat(response.expiresAt()).isEqualTo(savedChannel.getConnectTokenExpiresAt());
  }

  @Test
  void beginConnectionRejectsDisabledIntegration() {
    properties.setEnabled(false);

    assertThatThrownBy(() -> service.beginConnection(userWithId(UUID.randomUUID())))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Telegram integration is disabled");
  }

  @Test
  void completeConnectionConnectsChannelWhenTokenIsValid() {
    NotificationChannel channel = NotificationChannel.telegram(userWithId(UUID.randomUUID()));

    channel.beginConnection("token-hash", Instant.now().plus(Duration.ofMinutes(1)));

    when(tokenService.hash("raw-token")).thenReturn("token-hash");

    when(channelRepository.findByChannelTypeAndConnectTokenHash(ChannelType.TELEGRAM, "token-hash"))
        .thenReturn(Optional.of(channel));

    ConnectionAttempt result = service.completeConnection("raw-token", 123456789L, " bebo_user ");

    assertThat(result).isEqualTo(ConnectionAttempt.CONNECTED);

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.CONNECTED);

    assertThat(channel.getTelegramChatId()).isEqualTo(123456789L);

    assertThat(channel.getTelegramUsername()).isEqualTo("bebo_user");

    assertThat(channel.getExternalRecipientId()).isEqualTo("123456789");

    assertThat(channel.getConnectTokenHash()).isNull();

    assertThat(channel.getConnectTokenExpiresAt()).isNull();
  }

  @Test
  void completeConnectionExpiresChannelWhenTokenIsExpired() {
    NotificationChannel channel = NotificationChannel.telegram(userWithId(UUID.randomUUID()));

    channel.beginConnection("token-hash", Instant.now().minus(Duration.ofSeconds(1)));

    when(tokenService.hash("raw-token")).thenReturn("token-hash");

    when(channelRepository.findByChannelTypeAndConnectTokenHash(ChannelType.TELEGRAM, "token-hash"))
        .thenReturn(Optional.of(channel));

    ConnectionAttempt result = service.completeConnection("raw-token", 123456789L, "bebo_user");

    assertThat(result).isEqualTo(ConnectionAttempt.EXPIRED);

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.DISCONNECTED);

    assertThat(channel.getConnectTokenHash()).isNull();

    assertThat(channel.getConnectTokenExpiresAt()).isNull();
  }

  @Test
  void sendTestMessageUsesConnectedTelegramChat() {
    UUID userId = UUID.randomUUID();
    User user = userWithId(userId);

    NotificationChannel channel = NotificationChannel.telegram(user);

    channel.connectTelegram(123456789L, "bebo_user", Instant.now());

    TelegramBotClient telegramBotClient = mock(TelegramBotClient.class);

    when(channelRepository.findByUser_IdAndChannelType(userId, ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(telegramBotClientProvider.getIfAvailable()).thenReturn(telegramBotClient);

    TelegramTestResponse response = service.sendTestMessage(user);

    assertThat(response.sent()).isTrue();

    assertThat(response.sentAt()).isNotNull();

    verify(telegramBotClient)
        .sendMessage(
            123456789L,
            """
            bebo test notification

            Everything is connected correctly.
            Future cycle reminders will be sent here.
            """
                .strip());
  }

  @Test
  void disconnectClearsExistingTelegramChannel() {
    UUID userId = UUID.randomUUID();
    User user = userWithId(userId);

    NotificationChannel channel = NotificationChannel.telegram(user);

    channel.connectTelegram(123456789L, "bebo_user", Instant.now());

    when(channelRepository.findByUser_IdAndChannelType(userId, ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    service.disconnect(user);

    assertThat(channel.getConnectionStatus()).isEqualTo(NotificationChannelStatus.DISCONNECTED);

    assertThat(channel.getTelegramChatId()).isNull();

    assertThat(channel.getTelegramUsername()).isNull();

    assertThat(channel.getConnectedAt()).isNull();
  }

  private User userWithId(UUID id) {
    User user = User.create("test-" + id + "@example.com", "{noop}secret", "UTC");

    ReflectionTestUtils.setField(user, "id", id);

    return user;
  }
}
