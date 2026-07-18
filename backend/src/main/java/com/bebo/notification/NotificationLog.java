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

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

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

    return log;
  }

  public void markSent() {
    this.status = NotificationStatus.SENT;
    this.sentAt = Instant.now();
    this.errorMessage = null;
  }

  public void markFailed(String errorMessage) {
    this.status = NotificationStatus.FAILED;
    this.errorMessage = errorMessage;
  }
}
