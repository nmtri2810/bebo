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

@Entity
@Table(name = "notification_channels")
public class NotificationChannel extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 30)
  private ChannelType channelType;

  @Column(name = "external_recipient_id", nullable = false, length = 255)
  private String externalRecipientId;

  @Column(nullable = false)
  private boolean enabled;

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
