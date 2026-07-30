package com.bebo.notification.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.delivery.NotificationDispatcher;
import com.bebo.notification.discord.DiscordConnectionService.ConnectionAttempt;
import com.bebo.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiscordConnectionServiceTest {

  @Mock private NotificationChannelRepository channelRepository;

  @Mock private DiscordOAuthStateService stateService;

  @Mock private ObjectProvider<DiscordOAuthClient> oauthClientProvider;

  @Mock private NotificationDispatcher notificationDispatcher;

  @Mock private DiscordOAuthClient oauthClient;

  private DiscordProperties properties;

  private DiscordConnectionService service;

  @BeforeEach
  void setUp() {
    properties = new DiscordProperties();

    properties.setEnabled(true);
    properties.setClientId("client-id");
    properties.setClientSecret("client-secret");
    properties.setBotToken("bot-token");
    properties.setRedirectUri("http://localhost:8080/callback");
    properties.setFrontendRedirectUrl("http://localhost:3000/settings");
    properties.setStateTtl(Duration.ofMinutes(10));

    service =
        new DiscordConnectionService(
            channelRepository,
            stateService,
            properties,
            oauthClientProvider,
            notificationDispatcher);
  }

  @Test
  void completeConnectionReturnsAlreadyLinkedWhenDiscordAccountBelongsToAnotherUser() {
    NotificationChannel pendingChannel =
        NotificationChannel.discord(userWithId(UUID.randomUUID()));

    pendingChannel.beginConnection("state-hash", Instant.now().plus(Duration.ofMinutes(1)));

    NotificationChannel existingOwner = NotificationChannel.discord(userWithId(UUID.randomUUID()));

    existingOwner.connectDiscord("discord-user-id", "existing_user", Instant.now());

    when(stateService.hash("raw-state")).thenReturn("state-hash");

    when(channelRepository.findByChannelTypeAndConnectTokenHash(ChannelType.DISCORD, "state-hash"))
        .thenReturn(Optional.of(pendingChannel));

    when(oauthClientProvider.getIfAvailable()).thenReturn(oauthClient);

    when(oauthClient.exchangeAuthorizationCode("authorization-code")).thenReturn("access-token");

    when(oauthClient.getCurrentUser("access-token"))
        .thenReturn(new DiscordOAuthClient.DiscordUser("discord-user-id", "discord_user", null));

    when(channelRepository.findByChannelTypeAndExternalRecipientId(
            ChannelType.DISCORD, "discord-user-id"))
        .thenReturn(Optional.of(existingOwner));

    ConnectionAttempt result =
        service.completeConnection("authorization-code", "raw-state", null);

    assertThat(result).isEqualTo(ConnectionAttempt.ALREADY_LINKED);

    assertThat(pendingChannel.getConnectionStatus())
        .isEqualTo(NotificationChannelStatus.ALREADY_LINKED);

    assertThat(pendingChannel.isEnabled()).isFalse();

    assertThat(pendingChannel.getExternalRecipientId()).isNull();

    assertThat(pendingChannel.getExternalUsername()).isNull();

    assertThat(pendingChannel.getConnectTokenHash()).isNull();

    assertThat(pendingChannel.getConnectTokenExpiresAt()).isNull();

    verifyNoInteractions(notificationDispatcher);
  }

  private User userWithId(UUID id) {
    User user = User.create("test-" + id + "@example.com", "{noop}secret", "UTC");

    ReflectionTestUtils.setField(user, "id", id);

    return user;
  }
}
