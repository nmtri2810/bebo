package com.bebo.notification;

import com.bebo.common.model.BaseEntity;
import com.bebo.cycle.CycleRecord;
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
import java.time.LocalDate;

@Entity
@Table(name = "notification_logs")
public class NotificationLog extends BaseEntity {

  private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_record_id")
  private CycleRecord cycleRecord;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 30)
  private ChannelType channelType;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 50)
  private NotificationType notificationType;

  @Column(name = "predicted_period_date", nullable = false)
  private LocalDate predictedPeriodDate;

  @Column(name = "scheduled_for", nullable = false)
  private Instant scheduledFor;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NotificationStatus status;

  @Column(name = "error_message", length = MAX_ERROR_MESSAGE_LENGTH)
  private String errorMessage;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_attempt_at")
  private Instant lastAttemptAt;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  protected NotificationLog() {}

  public static NotificationLog createPending(
      User user,
      CycleRecord cycleRecord,
      ChannelType channelType,
      LocalDate predictedPeriodDate,
      Instant scheduledFor) {
    NotificationLog log = new NotificationLog();

    log.user = user;
    log.cycleRecord = cycleRecord;
    log.channelType = channelType;
    log.notificationType = NotificationType.CYCLE_APPROACHING;

    log.predictedPeriodDate = predictedPeriodDate;

    log.scheduledFor = scheduledFor;
    log.status = NotificationStatus.PENDING;
    log.attemptCount = 0;

    return log;
  }

  public void startAttempt(Instant attemptedAt) {
    this.status = NotificationStatus.PENDING;
    this.attemptCount += 1;
    this.lastAttemptAt = attemptedAt;
    this.nextRetryAt = null;
    this.errorMessage = null;
  }

  public void markSent(Instant sentAt) {
    this.status = NotificationStatus.SENT;
    this.sentAt = sentAt;
    this.nextRetryAt = null;
    this.errorMessage = null;
  }

  public void markFailed(String errorMessage, Instant nextRetryAt) {
    this.status = NotificationStatus.FAILED;
    this.sentAt = null;
    this.nextRetryAt = nextRetryAt;

    this.errorMessage = normalizeErrorMessage(errorMessage);
  }

  public void stopRetry(String reason) {
    markFailed(reason, null);
  }

  public void rescheduleRetry(Instant scheduledFor) {
    this.status = NotificationStatus.FAILED;
    this.scheduledFor = scheduledFor;
    this.nextRetryAt = scheduledFor;
  }

  public boolean isRetryDue(Instant now) {
    return status == NotificationStatus.FAILED && nextRetryAt != null && !nextRetryAt.isAfter(now);
  }

  private static String normalizeErrorMessage(String errorMessage) {
    if (errorMessage == null || errorMessage.isBlank()) {
      return "Unknown notification error";
    }

    String normalized = errorMessage.trim();

    if (normalized.length() <= MAX_ERROR_MESSAGE_LENGTH) {
      return normalized;
    }

    return normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
  }

  public User getUser() {
    return user;
  }

  public CycleRecord getCycleRecord() {
    return cycleRecord;
  }

  public ChannelType getChannelType() {
    return channelType;
  }

  public NotificationType getNotificationType() {
    return notificationType;
  }

  public LocalDate getPredictedPeriodDate() {
    return predictedPeriodDate;
  }

  public Instant getScheduledFor() {
    return scheduledFor;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public Instant getLastAttemptAt() {
    return lastAttemptAt;
  }

  public Instant getNextRetryAt() {
    return nextRetryAt;
  }
}
