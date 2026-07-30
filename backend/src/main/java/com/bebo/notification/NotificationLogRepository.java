package com.bebo.notification;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

  boolean existsByUserIdAndPredictedPeriodDateAndDeliveryLocalDateAndNotificationTypeAndChannelType(
      UUID userId,
      LocalDate predictedPeriodDate,
      LocalDate deliveryLocalDate,
      NotificationType notificationType,
      ChannelType channelType);

  @Query(
      """
      select log.id
      from NotificationLog log
      where log.status = :status
        and log.notificationType = :notificationType
        and log.nextRetryAt is not null
        and log.nextRetryAt <= :now
      order by log.nextRetryAt asc
      """)
  List<UUID> findDueRetryIds(
      @Param("status") NotificationStatus status,
      @Param("notificationType") NotificationType notificationType,
      @Param("now") Instant now,
      Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select log
      from NotificationLog log
      join fetch log.user
      left join fetch log.cycleRecord
      where log.id = :logId
      """)
  Optional<NotificationLog> findByIdForUpdate(@Param("logId") UUID logId);

  Page<NotificationLog> findAllByUser_IdAndStatusIn(
      UUID userId, Collection<NotificationStatus> statuses, Pageable pageable);
}
