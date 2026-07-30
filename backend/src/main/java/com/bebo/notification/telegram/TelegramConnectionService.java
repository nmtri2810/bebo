package com.bebo.notification.telegram;

import com.bebo.common.exception.BadRequestException;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.telegram.dto.TelegramConnectLinkResponse;
import com.bebo.notification.telegram.dto.TelegramConnectionResponse;
import com.bebo.notification.telegram.dto.TelegramTestResponse;
import com.bebo.user.User;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramConnectionService {

  private static final String TEST_MESSAGE =
      """
      bebo test notification

      Everything is connected correctly.
      Future cycle reminders will be sent here.
      """
          .strip();

  private final NotificationChannelRepository channelRepository;

  private final TelegramConnectionTokenService tokenService;

  private final TelegramProperties properties;

  private final ObjectProvider<TelegramBotClient> telegramBotClientProvider;

  public TelegramConnectionService(
      NotificationChannelRepository channelRepository,
      TelegramConnectionTokenService tokenService,
      TelegramProperties properties,
      ObjectProvider<TelegramBotClient> telegramBotClientProvider) {
    this.channelRepository = channelRepository;

    this.tokenService = tokenService;

    this.properties = properties;

    this.telegramBotClientProvider = telegramBotClientProvider;
  }

  @Transactional(readOnly = true)
  public TelegramConnectionResponse getStatus(User user) {
    return channelRepository
        .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
        .map(TelegramConnectionResponse::from)
        .orElseGet(TelegramConnectionResponse::disconnected);
  }

  @Transactional
  public TelegramConnectLinkResponse beginConnection(User user) {
    validateConnectionConfiguration();

    TelegramConnectionTokenService.IssuedToken token = tokenService.issue();

    Instant expiresAt = Instant.now().plus(properties.getConnectionTokenTtl());

    NotificationChannel channel =
        channelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
            .orElseGet(() -> NotificationChannel.telegram(user));

    channel.beginConnection(token.tokenHash(), expiresAt);

    channelRepository.save(channel);

    String username = properties.getBotUsername().trim().replaceFirst("^@", "");

    String deepLink = "https://t.me/" + username + "?start=" + token.rawToken();

    return new TelegramConnectLinkResponse(channel.getConnectionStatus(), deepLink, expiresAt);
  }

  @Transactional
  public ConnectionAttempt completeConnection(
      String rawToken, long telegramChatId, String telegramUsername) {
    String tokenHash = tokenService.hash(rawToken);

    NotificationChannel channel =
        channelRepository
            .findByChannelTypeAndConnectTokenHash(ChannelType.TELEGRAM, tokenHash)
            .orElse(null);

    if (channel == null) {
      return ConnectionAttempt.INVALID;
    }

    Instant now = Instant.now();

    if (channel.getConnectTokenExpiresAt() == null
        || !channel.getConnectTokenExpiresAt().isAfter(now)) {
      channel.expireConnection();

      return ConnectionAttempt.EXPIRED;
    }

    NotificationChannel existingOwner =
        channelRepository
            .findByChannelTypeAndTelegramChatId(ChannelType.TELEGRAM, telegramChatId)
            .orElse(null);

    if (existingOwner != null && !isSameChannel(existingOwner, channel)) {
      channel.markAlreadyLinked();

      return ConnectionAttempt.ALREADY_LINKED;
    }

    channel.connectTelegram(telegramChatId, normalizeUsername(telegramUsername), now);

    return ConnectionAttempt.CONNECTED;
  }

  public TelegramTestResponse sendTestMessage(User user) {
    validateSendConfiguration();

    NotificationChannel channel =
        channelRepository
            .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
            .filter(
                candidate ->
                    candidate.isEnabled()
                        && candidate.getConnectionStatus() == NotificationChannelStatus.CONNECTED
                        && candidate.getTelegramChatId() != null)
            .orElseThrow(
                () -> new BadRequestException("Telegram is not connected " + "for this user"));

    TelegramBotClient telegramBotClient = telegramBotClientProvider.getIfAvailable();

    if (telegramBotClient == null) {
      throw new BadRequestException("Telegram integration is disabled");
    }

    telegramBotClient.sendMessage(channel.getTelegramChatId(), TEST_MESSAGE);

    return new TelegramTestResponse(true, Instant.now());
  }

  @Transactional
  public void disconnect(User user) {
    channelRepository
        .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
        .ifPresent(NotificationChannel::disconnect);
  }

  private void validateConnectionConfiguration() {
    if (!properties.isEnabled()) {
      throw new BadRequestException("Telegram integration is disabled");
    }

    if (properties.getBotUsername().isBlank()) {
      throw new BadRequestException("Telegram bot username is not configured");
    }
  }

  private void validateSendConfiguration() {
    if (!properties.isEnabled()) {
      throw new BadRequestException("Telegram integration is disabled");
    }

    if (properties.getBotToken().isBlank()) {
      throw new BadRequestException("Telegram bot token is not configured");
    }
  }

  private String normalizeUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }

    return username.trim();
  }

  private boolean isSameChannel(NotificationChannel first, NotificationChannel second) {
    if (first == second) {
      return true;
    }

    if (first.getId() == null || second.getId() == null) {
      return false;
    }

    return first.getId().equals(second.getId());
  }

  public enum ConnectionAttempt {
    CONNECTED,
    ALREADY_LINKED,
    INVALID,
    EXPIRED
  }
}
