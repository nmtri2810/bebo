package com.bebo.notification;

import com.bebo.common.model.BaseEntity;
import com.bebo.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_channels")
public class NotificationChannel extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 30)
  private ChannelType channelType;

  @Column(name = "external_recipient_id", length = 255)
  private String externalRecipientId;

  @Column(nullable = false)
  private boolean enabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "connection_status", nullable = false, length = 20)
  private NotificationChannelStatus connectionStatus = NotificationChannelStatus.DISCONNECTED;

  @Column(name = "telegram_chat_id")
  private Long telegramChatId;

  @Column(name = "telegram_username", length = 100)
  private String telegramUsername;

  @Column(name = "connect_token_hash", length = 64)
  private String connectTokenHash;

  @Column(name = "connect_token_expires_at")
  private Instant connectTokenExpiresAt;

  @Column(name = "connected_at")
  private Instant connectedAt;

  protected NotificationChannel() {}

  private NotificationChannel(User user, ChannelType channelType, String externalRecipientId) {
    this.user = user;
    this.channelType = channelType;
    this.externalRecipientId = externalRecipientId;
    this.enabled = true;
  }

  public static NotificationChannel telegram(User user, String telegramChatId) {
    return new NotificationChannel(user, ChannelType.TELEGRAM, telegramChatId);
  }

  public static NotificationChannel telegram(User user) {
    NotificationChannel channel = new NotificationChannel();

    channel.user = user;
    channel.channelType = ChannelType.TELEGRAM;

    channel.connectionStatus = NotificationChannelStatus.DISCONNECTED;

    return channel;
  }

  public void beginConnection(String tokenHash, Instant expiresAt) {
    this.connectionStatus = NotificationChannelStatus.PENDING;

    this.connectTokenHash = tokenHash;
    this.connectTokenExpiresAt = expiresAt;
  }

  public void connectTelegram(long chatId, String username, Instant connectedAt) {
    this.telegramChatId = chatId;
    this.telegramUsername = username;
    this.externalRecipientId = Long.toString(chatId);

    this.connectionStatus = NotificationChannelStatus.CONNECTED;

    this.connectedAt = connectedAt;

    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public void expireConnection() {
    this.connectionStatus = NotificationChannelStatus.DISCONNECTED;

    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public void disconnect() {
    this.connectionStatus = NotificationChannelStatus.DISCONNECTED;

    this.telegramChatId = null;
    this.telegramUsername = null;
    this.connectedAt = null;

    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public NotificationChannelStatus getConnectionStatus() {
    return connectionStatus;
  }

  public Long getTelegramChatId() {
    return telegramChatId;
  }

  public String getTelegramUsername() {
    return telegramUsername;
  }

  public String getConnectTokenHash() {
    return connectTokenHash;
  }

  public Instant getConnectTokenExpiresAt() {
    return connectTokenExpiresAt;
  }

  public Instant getConnectedAt() {
    return connectedAt;
  }

  public void reconnect(String externalRecipientId) {
    this.externalRecipientId = externalRecipientId;
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  public User getUser() {
    return user;
  }

  public ChannelType getChannelType() {
    return channelType;
  }

  public String getExternalRecipientId() {
    return externalRecipientId;
  }

  public boolean isEnabled() {
    return enabled;
  }
}
