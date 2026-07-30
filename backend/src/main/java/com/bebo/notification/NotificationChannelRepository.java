package com.bebo.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

  Optional<NotificationChannel> findByUserIdAndChannelType(UUID userId, ChannelType channelType);

  List<NotificationChannel> findAllByChannelTypeAndEnabledTrue(ChannelType channelType);

  Optional<NotificationChannel> findByUser_IdAndChannelType(UUID userId, ChannelType channelType);

  Optional<NotificationChannel> findByChannelTypeAndConnectTokenHash(
      ChannelType channelType, String connectTokenHash);

  Optional<NotificationChannel> findByChannelTypeAndTelegramChatId(
      ChannelType channelType, Long telegramChatId);

  @Query(
      """
      select channel.id
      from NotificationChannel channel
      where channel.connectionStatus = :connectionStatus
        and channel.enabled = true
      """)
  List<UUID> findAllConnectedChannelIds(
      @Param("connectionStatus") NotificationChannelStatus connectionStatus);
}
