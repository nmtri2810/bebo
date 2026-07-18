package com.bebo.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, UUID> {

  Optional<NotificationChannel> findByUserIdAndChannelType(UUID userId, ChannelType channelType);

  List<NotificationChannel> findAllByChannelTypeAndEnabledTrue(ChannelType channelType);
}
