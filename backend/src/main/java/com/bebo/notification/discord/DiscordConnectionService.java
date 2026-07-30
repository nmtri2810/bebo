package com.bebo.notification.discord;

import com.bebo.common.exception.BadRequestException;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationDispatcher;
import com.bebo.notification.discord.DiscordOAuthClient.DiscordUser;
import com.bebo.notification.discord.dto.DiscordConnectLinkResponse;
import com.bebo.notification.discord.dto.DiscordConnectionResponse;
import com.bebo.notification.discord.dto.DiscordTestResponse;
import com.bebo.user.User;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DiscordConnectionService {

  private static final Logger log = LoggerFactory.getLogger(DiscordConnectionService.class);

  private static final String CONNECTION_MESSAGE =
      """
      bebo Discord connection

      Everything is connected correctly.
      Future cycle reminders can now be sent privately through Discord.
      """
          .strip();

  private static final String TEST_MESSAGE =
      """
      bebo test notification

      Everything is connected correctly.
      Future cycle reminders will be sent here.
      """
          .strip();

  private final NotificationChannelRepository channelRepository;

  private final DiscordOAuthStateService stateService;

  private final DiscordProperties properties;

  private final ObjectProvider<DiscordOAuthClient> oauthClientProvider;

  private final NotificationDispatcher notificationDispatcher;

  public DiscordConnectionService(
      NotificationChannelRepository channelRepository,
      DiscordOAuthStateService stateService,
      DiscordProperties properties,
      ObjectProvider<DiscordOAuthClient> oauthClientProvider,
      NotificationDispatcher notificationDispatcher) {
    this.channelRepository = channelRepository;

    this.stateService = stateService;

    this.properties = properties;

    this.oauthClientProvider = oauthClientProvider;

    this.notificationDispatcher = notificationDispatcher;
  }

  @Transactional(readOnly = true)
  public DiscordConnectionResponse getStatus(User user) {
    return channelRepository
        .findByUser_IdAndChannelType(user.getId(), ChannelType.DISCORD)
        .map(DiscordConnectionResponse::from)
        .orElseGet(DiscordConnectionResponse::disconnected);
  }

  @Transactional
  public DiscordConnectLinkResponse beginConnection(User user) {
    validateConfiguration();

    DiscordOAuthStateService.IssuedState issuedState = stateService.issue();

    Instant expiresAt = Instant.now().plus(properties.getStateTtl());

    NotificationChannel channel =
        channelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.DISCORD)
            .orElseGet(() -> NotificationChannel.discord(user));

    channel.beginConnection(issuedState.stateHash(), expiresAt);

    channelRepository.save(channel);

    String authorizationUrl =
        UriComponentsBuilder.fromUriString("https://discord.com/oauth2/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", properties.getClientId())
            .queryParam("scope", "identify applications.commands")
            .queryParam("state", issuedState.rawState())
            .queryParam("redirect_uri", properties.getRedirectUri())
            .queryParam("prompt", "consent")
            .queryParam("integration_type", "1")
            .build()
            .encode()
            .toUriString();

    return new DiscordConnectLinkResponse(
        channel.getConnectionStatus(), authorizationUrl, expiresAt);
  }

  @Transactional
  public ConnectionAttempt completeConnection(
      String authorizationCode, String rawState, String oauthError) {
    if (rawState == null || rawState.isBlank()) {
      return ConnectionAttempt.INVALID;
    }

    String stateHash = stateService.hash(rawState);

    NotificationChannel channel =
        channelRepository
            .findByChannelTypeAndConnectTokenHash(ChannelType.DISCORD, stateHash)
            .orElse(null);

    if (channel == null) {
      return ConnectionAttempt.INVALID;
    }

    if (oauthError != null && !oauthError.isBlank()) {
      channel.disconnect();

      return ConnectionAttempt.DENIED;
    }

    Instant now = Instant.now();

    if (channel.getConnectTokenExpiresAt() == null
        || !channel.getConnectTokenExpiresAt().isAfter(now)) {
      channel.expireConnection();

      return ConnectionAttempt.EXPIRED;
    }

    if (authorizationCode == null || authorizationCode.isBlank()) {
      channel.disconnect();

      return ConnectionAttempt.INVALID;
    }

    try {
      DiscordOAuthClient oauthClient = requireOAuthClient();

      String accessToken = oauthClient.exchangeAuthorizationCode(authorizationCode);

      DiscordUser discordUser = oauthClient.getCurrentUser(accessToken);

      NotificationChannel existingOwner =
          channelRepository
              .findByChannelTypeAndExternalRecipientId(ChannelType.DISCORD, discordUser.id())
              .orElse(null);

      if (existingOwner != null && !existingOwner.getId().equals(channel.getId())) {
        channel.markAlreadyLinked();

        return ConnectionAttempt.ALREADY_LINKED;
      }

      notificationDispatcher.send(
          ChannelType.DISCORD,
          new NotificationDeliveryRequest(discordUser.id(), CONNECTION_MESSAGE));

      channel.connectDiscord(discordUser.id(), discordUser.username(), now);

      return ConnectionAttempt.CONNECTED;
    } catch (RuntimeException exception) {
      log.warn(
          "Could not complete Discord connection for notification channel {}",
          channel.getId(),
          exception);

      channel.disconnect();

      return ConnectionAttempt.DELIVERY_FAILED;
    }
  }

  public DiscordTestResponse sendTestMessage(User user) {
    validateConfiguration();

    NotificationChannel channel =
        channelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.DISCORD)
            .filter(
                candidate ->
                    candidate.isEnabled()
                        && candidate.getConnectionStatus() == NotificationChannelStatus.CONNECTED
                        && candidate.getExternalRecipientId() != null)
            .orElseThrow(() -> new BadRequestException("Discord is not connected for this user"));

    notificationDispatcher.send(
        ChannelType.DISCORD,
        new NotificationDeliveryRequest(channel.getExternalRecipientId(), TEST_MESSAGE));

    return new DiscordTestResponse(true, Instant.now());
  }

  @Transactional
  public void disconnect(User user) {
    channelRepository
        .findByUser_IdAndChannelType(user.getId(), ChannelType.DISCORD)
        .ifPresent(NotificationChannel::disconnect);
  }

  private DiscordOAuthClient requireOAuthClient() {
    DiscordOAuthClient client = oauthClientProvider.getIfAvailable();

    if (client == null) {
      throw new BadRequestException("Discord integration is disabled");
    }

    return client;
  }

  private void validateConfiguration() {
    if (!properties.isEnabled()) {
      throw new BadRequestException("Discord integration is disabled");
    }

    requireConfigured(properties.getClientId(), "Discord client ID");

    requireConfigured(properties.getClientSecret(), "Discord client secret");

    requireConfigured(properties.getBotToken(), "Discord bot token");

    requireConfigured(properties.getRedirectUri(), "Discord redirect URI");

    requireConfigured(properties.getFrontendRedirectUrl(), "Discord frontend redirect URL");

    if (!notificationDispatcher.supports(ChannelType.DISCORD)) {
      throw new BadRequestException("Discord notification sender is unavailable");
    }

    requireOAuthClient();
  }

  private void requireConfigured(String value, String propertyName) {
    if (value == null || value.isBlank()) {
      throw new BadRequestException(propertyName + " is not configured");
    }
  }

  public enum ConnectionAttempt {
    CONNECTED("connected"),
    ALREADY_LINKED("already_linked"),
    INVALID("invalid"),
    EXPIRED("expired"),
    DENIED("denied"),
    DELIVERY_FAILED("delivery_failed");

    private final String queryValue;

    ConnectionAttempt(String queryValue) {
      this.queryValue = queryValue;
    }

    public String queryValue() {
      return queryValue;
    }
  }
}
