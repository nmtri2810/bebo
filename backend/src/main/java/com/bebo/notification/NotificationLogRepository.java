package com.bebo.notification;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

  boolean existsByUserIdAndPredictedPeriodDateAndNotificationTypeAndChannelType(
      UUID userId,
      LocalDate predictedPeriodDate,
      NotificationType notificationType,
      ChannelType channelType);
}
