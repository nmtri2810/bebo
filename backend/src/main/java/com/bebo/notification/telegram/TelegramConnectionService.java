package com.bebo.notification.telegram;

import com.bebo.common.exception.BadRequestException;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.telegram.dto.TelegramConnectLinkResponse;
import com.bebo.notification.telegram.dto.TelegramConnectionResponse;
import com.bebo.user.User;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelegramConnectionService {

  private final NotificationChannelRepository channelRepository;

  private final TelegramConnectionTokenService tokenService;

  private final TelegramProperties properties;

  public TelegramConnectionService(
      NotificationChannelRepository channelRepository,
      TelegramConnectionTokenService tokenService,
      TelegramProperties properties) {
    this.channelRepository = channelRepository;

    this.tokenService = tokenService;
    this.properties = properties;
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
    validateConfiguration();

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

    channel.connectTelegram(telegramChatId, normalizeUsername(telegramUsername), now);

    return ConnectionAttempt.CONNECTED;
  }

  @Transactional
  public void disconnect(User user) {
    channelRepository
        .findByUser_IdAndChannelType(user.getId(), ChannelType.TELEGRAM)
        .ifPresent(NotificationChannel::disconnect);
  }

  private void validateConfiguration() {
    if (!properties.isEnabled()) {
      throw new BadRequestException("Telegram integration is disabled");
    }

    if (properties.getBotUsername().isBlank()) {
      throw new BadRequestException("Telegram bot username is not configured");
    }
  }

  private String normalizeUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }

    return username.trim();
  }

  public enum ConnectionAttempt {
    CONNECTED,
    INVALID,
    EXPIRED
  }
}
