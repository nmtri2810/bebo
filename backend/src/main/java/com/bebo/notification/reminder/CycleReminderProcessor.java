package com.bebo.notification.reminder;

import com.bebo.cycle.CyclePredictionCalculator;
import com.bebo.cycle.CycleRecord;
import com.bebo.cycle.CycleRecordRepository;
import com.bebo.cycle.CycleRecordRepository.CycleStartProjection;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationType;
import com.bebo.notification.telegram.TelegramBotClient;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "bebo.telegram", name = "enabled", havingValue = "true")
public class CycleReminderProcessor {

  private static final int MAX_RECENT_RECORDS = 7;

  private static final DateTimeFormatter MESSAGE_DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ENGLISH);

  private final NotificationChannelRepository notificationChannelRepository;

  private final NotificationLogRepository notificationLogRepository;

  private final CycleRecordRepository cycleRecordRepository;

  private final CycleSettingsRepository cycleSettingsRepository;

  private final TelegramBotClient telegramBotClient;

  public CycleReminderProcessor(
      NotificationChannelRepository notificationChannelRepository,
      NotificationLogRepository notificationLogRepository,
      CycleRecordRepository cycleRecordRepository,
      CycleSettingsRepository cycleSettingsRepository,
      TelegramBotClient telegramBotClient) {
    this.notificationChannelRepository = notificationChannelRepository;

    this.notificationLogRepository = notificationLogRepository;

    this.cycleRecordRepository = cycleRecordRepository;

    this.cycleSettingsRepository = cycleSettingsRepository;

    this.telegramBotClient = telegramBotClient;
  }

  @Transactional
  public void process(UUID channelId, Instant now) {
    NotificationChannel channel = notificationChannelRepository.findById(channelId).orElse(null);

    if (!isUsableTelegramChannel(channel)) {
      return;
    }

    User user = channel.getUser();

    if (!user.isActive()) {
      return;
    }

    CycleSettings settings = cycleSettingsRepository.findByUser_Id(user.getId()).orElse(null);

    if (settings == null) {
      return;
    }

    List<CycleStartProjection> recentStarts =
        cycleRecordRepository.findRecentCycleStarts(
            user.getId(), PageRequest.of(0, MAX_RECENT_RECORDS));

    if (recentStarts.isEmpty()) {
      return;
    }

    List<LocalDate> startDates =
        recentStarts.stream().map(CycleStartProjection::getStartDate).toList();

    CyclePredictionCalculator.Calculation calculation =
        CyclePredictionCalculator.calculateAverage(startDates, settings.getDefaultCycleLength());

    CycleStartProjection latestRecord = recentStarts.getFirst();

    LocalDate predictedPeriodDate =
        latestRecord.getStartDate().plusDays(calculation.averageCycleLength());

    LocalDate reminderDate = predictedPeriodDate.minusDays(settings.getReminderDaysBefore());

    ZoneId userZone = ZoneId.of(user.getTimezone());

    ZonedDateTime localNow = now.atZone(userZone);

    if (!localNow.toLocalDate().equals(reminderDate)) {
      return;
    }

    ZonedDateTime localScheduledTime =
        ZonedDateTime.of(reminderDate, settings.getNotificationTime(), userZone);

    Instant scheduledFor = localScheduledTime.toInstant();

    if (now.isBefore(scheduledFor)) {
      return;
    }

    boolean alreadyProcessed =
        notificationLogRepository
            .existsByUserIdAndPredictedPeriodDateAndNotificationTypeAndChannelType(
                user.getId(),
                predictedPeriodDate,
                NotificationType.CYCLE_APPROACHING,
                ChannelType.TELEGRAM);

    if (alreadyProcessed) {
      return;
    }

    CycleRecord cycleRecord = cycleRecordRepository.getReferenceById(latestRecord.getId());

    NotificationLog notificationLog =
        NotificationLog.createPending(
            user, cycleRecord, ChannelType.TELEGRAM, predictedPeriodDate, scheduledFor);

    /*
     * Flush trước khi gọi Telegram để DB
     * reserve unique dedup key.
     */
    notificationLogRepository.saveAndFlush(notificationLog);

    try {
      telegramBotClient.sendMessage(
          channel.getTelegramChatId(),
          buildMessage(predictedPeriodDate, settings.getReminderDaysBefore()));

      notificationLog.markSent(now);
    } catch (RuntimeException exception) {
      notificationLog.markFailed(getErrorMessage(exception));
    }
  }

  private boolean isUsableTelegramChannel(NotificationChannel channel) {
    return channel != null
        && channel.getChannelType() == ChannelType.TELEGRAM
        && channel.isEnabled()
        && channel.getConnectionStatus() == NotificationChannelStatus.CONNECTED
        && channel.getTelegramChatId() != null;
  }

  private String buildMessage(LocalDate predictedPeriodDate, int reminderDaysBefore) {
    String reminderDescription;

    if (reminderDaysBefore == 0) {
      reminderDescription = "The next period is estimated " + "to start today.";
    } else if (reminderDaysBefore == 1) {
      reminderDescription = "The next period is estimated " + "to start tomorrow.";
    } else {
      reminderDescription =
          "The next period is estimated " + "to start in " + reminderDaysBefore + " days.";
    }

    return """
    bebo cycle reminder

    %s
    Estimated start: %s

    Predictions are estimates and may change as more cycle data is added.
    """
        .formatted(reminderDescription, predictedPeriodDate.format(MESSAGE_DATE_FORMATTER))
        .strip();
  }

  private String getErrorMessage(RuntimeException exception) {
    String message = exception.getMessage();

    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }

    return exception.getClass().getSimpleName() + ": " + message;
  }
}
