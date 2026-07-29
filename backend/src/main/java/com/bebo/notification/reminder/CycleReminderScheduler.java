package com.bebo.notification.reminder;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class CycleReminderScheduler {

  private static final Logger log = LoggerFactory.getLogger(CycleReminderScheduler.class);

  private final NotificationChannelRepository notificationChannelRepository;

  private final CycleReminderProcessor cycleReminderProcessor;

  private final ReminderProperties reminderProperties;

  public CycleReminderScheduler(
      NotificationChannelRepository notificationChannelRepository,
      CycleReminderProcessor cycleReminderProcessor,
      ReminderProperties reminderProperties) {
    this.notificationChannelRepository = notificationChannelRepository;

    this.cycleReminderProcessor = cycleReminderProcessor;

    this.reminderProperties = reminderProperties;
  }

  @Scheduled(cron = "${bebo.reminder.cron:" + "0 * * * * *}", zone = "UTC")
  public void sendDueReminders() {
    if (!reminderProperties.isEnabled()) {
      return;
    }

    Instant now = Instant.now();

    List<UUID> channelIds =
        notificationChannelRepository.findAllConnectedChannelIds(
            ChannelType.TELEGRAM, NotificationChannelStatus.CONNECTED);

    for (UUID channelId : channelIds) {
      try {
        cycleReminderProcessor.process(channelId, now);
      } catch (RuntimeException exception) {
        log.error(
            "Could not process cycle reminder " + "for notification channel {}",
            channelId,
            exception);
      }
    }
  }
}
