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

  public static NotificationChannel telegram(User user, String telegramChatId) {
    NotificationChannel channel = telegram(user);

    channel.connectTelegram(Long.parseLong(telegramChatId), null, Instant.now());

    return channel;
  }

  public static NotificationChannel telegram(User user) {
    NotificationChannel channel = new NotificationChannel();

    channel.user = user;
    channel.channelType = ChannelType.TELEGRAM;
    channel.enabled = false;
    channel.connectionStatus = NotificationChannelStatus.DISCONNECTED;

    return channel;
  }

  public void beginConnection(String tokenHash, Instant expiresAt) {
    this.enabled = false;
    this.connectionStatus = NotificationChannelStatus.PENDING;

    /*
     * Một lần kết nối mới thay thế trạng thái
     * Telegram cũ của channel này.
     */
    this.externalRecipientId = null;
    this.telegramChatId = null;
    this.telegramUsername = null;
    this.connectedAt = null;

    this.connectTokenHash = tokenHash;
    this.connectTokenExpiresAt = expiresAt;
  }

  public void connectTelegram(long chatId, String username, Instant connectedAt) {
    this.telegramChatId = chatId;
    this.telegramUsername = username;
    this.externalRecipientId = Long.toString(chatId);

    this.enabled = true;
    this.connectionStatus = NotificationChannelStatus.CONNECTED;

    this.connectedAt = connectedAt;

    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public void markAlreadyLinked() {
    this.enabled = false;
    this.connectionStatus = NotificationChannelStatus.ALREADY_LINKED;

    /*
     * Không lưu Telegram chat ID vào account thứ hai,
     * tránh vi phạm unique constraint.
     */
    this.externalRecipientId = null;
    this.telegramChatId = null;
    this.telegramUsername = null;
    this.connectedAt = null;

    /*
     * Token đã được sử dụng và không thể dùng lại.
     */
    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public void expireConnection() {
    disconnect();
  }

  public void disconnect() {
    this.enabled = false;
    this.connectionStatus = NotificationChannelStatus.DISCONNECTED;

    this.externalRecipientId = null;
    this.telegramChatId = null;
    this.telegramUsername = null;
    this.connectedAt = null;

    this.connectTokenHash = null;
    this.connectTokenExpiresAt = null;
  }

  public void reconnect(String externalRecipientId) {
    long chatId = Long.parseLong(externalRecipientId);

    connectTelegram(chatId, this.telegramUsername, Instant.now());
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
}
