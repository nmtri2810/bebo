package com.bebo.notification.reminder;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class FailedNotificationRetryScheduler {

  private static final Logger log = LoggerFactory.getLogger(FailedNotificationRetryScheduler.class);

  private final NotificationLogRepository notificationLogRepository;

  private final FailedNotificationRetryProcessor retryProcessor;

  private final ReminderProperties reminderProperties;

  public FailedNotificationRetryScheduler(
      NotificationLogRepository notificationLogRepository,
      FailedNotificationRetryProcessor retryProcessor,
      ReminderProperties reminderProperties) {
    this.notificationLogRepository = notificationLogRepository;

    this.retryProcessor = retryProcessor;

    this.reminderProperties = reminderProperties;
  }

  @Scheduled(cron = "${bebo.reminder.retry-cron:" + "15 * * * * *}", zone = "UTC")
  public void retryFailedNotifications() {
    if (!reminderProperties.isEnabled()) {
      return;
    }

    Instant now = Instant.now();

    List<UUID> notificationLogIds =
        notificationLogRepository.findDueRetryIds(
            NotificationStatus.FAILED,
            ChannelType.TELEGRAM,
            NotificationType.CYCLE_APPROACHING,
            now,
            PageRequest.of(0, reminderProperties.getRetryBatchSize()));

    for (UUID notificationLogId : notificationLogIds) {
      try {
        retryProcessor.retry(notificationLogId, now);
      } catch (RuntimeException exception) {
        log.error("Could not retry notification {}", notificationLogId, exception);
      }
    }
  }
}
