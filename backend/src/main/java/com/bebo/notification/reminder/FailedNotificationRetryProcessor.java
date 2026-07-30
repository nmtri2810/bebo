package com.bebo.notification.reminder;

import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationType;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationDispatcher;
import com.bebo.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailedNotificationRetryProcessor {

  private final NotificationLogRepository notificationLogRepository;

  private final NotificationChannelRepository notificationChannelRepository;

  private final CycleReminderPlanService cycleReminderPlanService;

  private final NotificationRetryPolicy notificationRetryPolicy;

  private final NotificationDispatcher notificationDispatcher;

  public FailedNotificationRetryProcessor(
      NotificationLogRepository notificationLogRepository,
      NotificationChannelRepository notificationChannelRepository,
      CycleReminderPlanService cycleReminderPlanService,
      NotificationRetryPolicy notificationRetryPolicy,
      NotificationDispatcher notificationDispatcher) {
    this.notificationLogRepository = notificationLogRepository;

    this.notificationChannelRepository = notificationChannelRepository;

    this.cycleReminderPlanService = cycleReminderPlanService;

    this.notificationRetryPolicy = notificationRetryPolicy;

    this.notificationDispatcher = notificationDispatcher;
  }

  @Transactional
  public void retry(UUID notificationLogId, Instant now) {
    NotificationLog notificationLog =
        notificationLogRepository.findByIdForUpdate(notificationLogId).orElse(null);

    if (notificationLog == null || !notificationLog.isRetryDue(now)) {
      return;
    }

    if (notificationLog.getNotificationType() != NotificationType.CYCLE_APPROACHING) {
      notificationLog.stopRetry("Retry stopped because notification type is unsupported.");

      return;
    }

    User user = notificationLog.getUser();

    if (!user.isActive()) {
      notificationLog.stopRetry("Retry stopped because the user is inactive.");

      return;
    }

    NotificationChannel channel =
        notificationChannelRepository
            .findByUser_IdAndChannelType(user.getId(), notificationLog.getChannelType())
            .orElse(null);

    if (!isConnectedChannel(channel)) {
      notificationLog.stopRetry(
          "Retry stopped because the notification channel is disconnected or disabled.");

      return;
    }

    /*
     * Provider có thể bị tắt tạm thời
     * trong deployment.
     *
     * Giữ nguyên retry để tiếp tục khi
     * NotificationSender được bật lại.
     */
    if (!notificationDispatcher.supports(notificationLog.getChannelType())) {
      return;
    }

    CycleReminderPlan currentPlan = cycleReminderPlanService.createPlan(user).orElse(null);

    if (currentPlan == null) {
      notificationLog.stopRetry("Retry stopped because cycle prediction is unavailable.");

      return;
    }

    if (notificationLog.getCycleRecord() == null
        || !currentPlan.latestCycleRecordId().equals(notificationLog.getCycleRecord().getId())) {
      notificationLog.stopRetry("Retry stopped because a newer cycle record exists.");

      return;
    }

    if (!currentPlan.predictedPeriodDate().equals(notificationLog.getPredictedPeriodDate())) {
      notificationLog.stopRetry("Retry stopped because the cycle prediction has changed.");

      return;
    }

    LocalDate deliveryLocalDate = notificationLog.getDeliveryLocalDate();

    if (!currentPlan.includesDeliveryDate(deliveryLocalDate)) {
      notificationLog.stopRetry(
          "Retry stopped because this date is outside the current reminder window.");

      return;
    }

    ZoneId userZone = ZoneId.of(user.getTimezone());

    LocalDate currentLocalDate = now.atZone(userZone).toLocalDate();

    if (currentLocalDate.isAfter(deliveryLocalDate)) {
      notificationLog.stopRetry("Retry stopped because the daily reminder is stale.");

      return;
    }

    Instant currentScheduledFor = currentPlan.scheduledFor(deliveryLocalDate, userZone);

    if (currentLocalDate.isBefore(deliveryLocalDate) || now.isBefore(currentScheduledFor)) {
      notificationLog.rescheduleRetry(currentScheduledFor);

      return;
    }

    notificationLog.startAttempt(now);

    try {
      notificationDispatcher.send(
          notificationLog.getChannelType(),
          new NotificationDeliveryRequest(
              channel.getExternalRecipientId(), notificationLog.getMessageBody()));

      notificationLog.markSent(now);
    } catch (RuntimeException exception) {
      Instant nextRetryAt =
          notificationRetryPolicy
              .nextRetryAtAfterFailure(notificationLog.getAttemptCount(), now)
              .orElse(null);

      notificationLog.markFailed(getErrorMessage(exception), nextRetryAt);
    }
  }

  private boolean isConnectedChannel(NotificationChannel channel) {
    return channel != null
        && channel.isEnabled()
        && channel.getConnectionStatus() == NotificationChannelStatus.CONNECTED
        && channel.getExternalRecipientId() != null
        && !channel.getExternalRecipientId().isBlank();
  }

  private String getErrorMessage(RuntimeException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }

    return exception.getClass().getSimpleName() + ": " + message;
  }
}
