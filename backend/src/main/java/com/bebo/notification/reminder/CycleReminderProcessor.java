package com.bebo.notification.reminder;

import com.bebo.cycle.CycleRecord;
import com.bebo.cycle.CycleRecordRepository;
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
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleReminderProcessor {

  private final NotificationChannelRepository notificationChannelRepository;

  private final NotificationLogRepository notificationLogRepository;

  private final CycleRecordRepository cycleRecordRepository;

  private final CycleReminderPlanService cycleReminderPlanService;

  private final CycleReminderMessageBuilder messageBuilder;

  private final NotificationRetryPolicy notificationRetryPolicy;

  private final NotificationDispatcher notificationDispatcher;

  public CycleReminderProcessor(
      NotificationChannelRepository notificationChannelRepository,
      NotificationLogRepository notificationLogRepository,
      CycleRecordRepository cycleRecordRepository,
      CycleReminderPlanService cycleReminderPlanService,
      CycleReminderMessageBuilder messageBuilder,
      NotificationRetryPolicy notificationRetryPolicy,
      NotificationDispatcher notificationDispatcher) {
    this.notificationChannelRepository = notificationChannelRepository;

    this.notificationLogRepository = notificationLogRepository;

    this.cycleRecordRepository = cycleRecordRepository;

    this.cycleReminderPlanService = cycleReminderPlanService;

    this.messageBuilder = messageBuilder;

    this.notificationRetryPolicy = notificationRetryPolicy;

    this.notificationDispatcher = notificationDispatcher;
  }

  @Transactional
  public void process(UUID channelId, Instant now) {
    NotificationChannel channel = notificationChannelRepository.findById(channelId).orElse(null);

    if (!isUsableChannel(channel)) {
      return;
    }

    User user = channel.getUser();

    if (!user.isActive()) {
      return;
    }

    CycleReminderPlan plan = cycleReminderPlanService.createPlan(user).orElse(null);

    if (plan == null) {
      return;
    }

    ZoneId userZone = ZoneId.of(user.getTimezone());

    CycleReminderPlan.Delivery delivery = plan.createDueDelivery(now, userZone).orElse(null);

    if (delivery == null) {
      return;
    }

    boolean alreadyProcessed =
        notificationLogRepository
            .existsByUserIdAndPredictedPeriodDateAndDeliveryLocalDateAndNotificationTypeAndChannelType(
                user.getId(),
                plan.predictedPeriodDate(),
                delivery.deliveryLocalDate(),
                NotificationType.CYCLE_APPROACHING,
                channel.getChannelType());

    if (alreadyProcessed) {
      return;
    }

    CycleRecord cycleRecord = cycleRecordRepository.getReferenceById(plan.latestCycleRecordId());

    String messageBody =
        messageBuilder.build(
            plan.predictedPeriodDate(), delivery.stage(), delivery.daysRelativeToPrediction());

    NotificationLog notificationLog =
        NotificationLog.createPending(
            user,
            cycleRecord,
            channel.getChannelType(),
            plan.predictedPeriodDate(),
            delivery.deliveryLocalDate(),
            delivery.stage(),
            delivery.daysRelativeToPrediction(),
            messageBody,
            delivery.scheduledFor());

    notificationLog.startAttempt(now);

    /*
     * Flush trước khi gửi để reserve
     * unique key theo user, channel,
     * prediction và local delivery date.
     */
    notificationLog = notificationLogRepository.saveAndFlush(notificationLog);

    try {
      notificationDispatcher.send(
          channel.getChannelType(),
          new NotificationDeliveryRequest(channel.getExternalRecipientId(), messageBody));

      notificationLog.markSent(now);
    } catch (RuntimeException exception) {
      Instant nextRetryAt =
          notificationRetryPolicy
              .nextRetryAtAfterFailure(notificationLog.getAttemptCount(), now)
              .orElse(null);

      notificationLog.markFailed(getErrorMessage(exception), nextRetryAt);
    }
  }

  private boolean isUsableChannel(NotificationChannel channel) {
    return channel != null
        && channel.isEnabled()
        && channel.getConnectionStatus() == NotificationChannelStatus.CONNECTED
        && channel.getExternalRecipientId() != null
        && !channel.getExternalRecipientId().isBlank()
        && notificationDispatcher.supports(channel.getChannelType());
  }

  private String getErrorMessage(RuntimeException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }

    return exception.getClass().getSimpleName() + ": " + message;
  }
}
