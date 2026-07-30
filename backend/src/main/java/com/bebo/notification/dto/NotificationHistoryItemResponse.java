package com.bebo.notification.dto;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.NotificationType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NotificationHistoryItemResponse(
    UUID id,
    ChannelType channelType,
    NotificationType notificationType,
    LocalDate predictedPeriodDate,
    Instant scheduledFor,
    Instant sentAt,
    NotificationStatus status,
    int attemptCount,
    Instant lastAttemptAt,
    Instant nextRetryAt,
    String failureMessage) {

  private static final String SAFE_FAILURE_MESSAGE =
      "This notification channel could not deliver the reminder.";

  public static NotificationHistoryItemResponse from(NotificationLog log) {
    return new NotificationHistoryItemResponse(
        log.getId(),
        log.getChannelType(),
        log.getNotificationType(),
        log.getPredictedPeriodDate(),
        log.getScheduledFor(),
        log.getSentAt(),
        log.getStatus(),
        log.getAttemptCount(),
        log.getLastAttemptAt(),
        log.getNextRetryAt(),
        getSafeFailureMessage(log));
  }

  private static String getSafeFailureMessage(NotificationLog log) {
    if (log.getStatus() != NotificationStatus.FAILED) {
      return null;
    }

    /*
     * Không trả raw error_message
     * ra frontend.
     *
     * Exception HTTP có thể chứa URL,
     * token hoặc thông tin nội bộ.
     */
    return SAFE_FAILURE_MESSAGE;
  }
}
