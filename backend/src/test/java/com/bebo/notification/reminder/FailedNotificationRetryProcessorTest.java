package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bebo.cycle.CycleRecord;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.telegram.TelegramBotClient;
import com.bebo.user.User;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FailedNotificationRetryProcessorTest {

  private static final String MESSAGE_BODY = "Stored daily reminder message";

  @Mock private NotificationLogRepository notificationLogRepository;

  @Mock private NotificationChannelRepository notificationChannelRepository;

  @Mock private CycleReminderPlanService cycleReminderPlanService;

  @Mock private NotificationRetryPolicy notificationRetryPolicy;

  @Mock private TelegramBotClient telegramBotClient;

  private FailedNotificationRetryProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new FailedNotificationRetryProcessor(
            notificationLogRepository,
            notificationChannelRepository,
            cycleReminderPlanService,
            notificationRetryPolicy,
            telegramBotClient);
  }

  @Test
  void retrySendsStoredDailyMessageAndMarksLogSent() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minus(Duration.ofSeconds(1)));

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = currentPlan(cycleRecordId, LocalTime.of(8, 0));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    processor.retry(logId, now);

    verify(telegramBotClient).sendMessage(123456789L, MESSAGE_BODY);

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.SENT);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(2);

    assertThat(notificationLog.getLastAttemptAt()).isEqualTo(now);

    assertThat(notificationLog.getSentAt()).isEqualTo(now);

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage()).isNull();
  }

  @Test
  void retryMarksLogFailedAndSchedulesNextRetryWhenTelegramSendFails() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    Instant nextRetryAt = now.plus(Duration.ofMinutes(15));

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minus(Duration.ofSeconds(1)));

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = currentPlan(cycleRecordId, LocalTime.of(8, 0));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    doThrow(new IllegalStateException("Telegram is down"))
        .when(telegramBotClient)
        .sendMessage(123456789L, MESSAGE_BODY);

    when(notificationRetryPolicy.nextRetryAtAfterFailure(2, now))
        .thenReturn(Optional.of(nextRetryAt));

    processor.retry(logId, now);

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(2);

    assertThat(notificationLog.getLastAttemptAt()).isEqualTo(now);

    assertThat(notificationLog.getSentAt()).isNull();

    assertThat(notificationLog.getNextRetryAt()).isEqualTo(nextRetryAt);

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("IllegalStateException: Telegram is down");
  }

  @Test
  void retryStopsWhenTelegramIsDisconnected() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minus(Duration.ofSeconds(1)));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.empty());

    processor.retry(logId, now);

    verify(telegramBotClient, never()).sendMessage(any(Long.class), any(String.class));

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("Retry stopped because Telegram is disconnected or disabled.");
  }

  @Test
  void retryReschedulesWhenDailyNotificationTimeMovedToFuture() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    Instant rescheduledFor = Instant.parse("2026-07-29T09:00:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minus(Duration.ofSeconds(1)));

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = currentPlan(cycleRecordId, LocalTime.of(9, 0));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    processor.retry(logId, now);

    verify(telegramBotClient, never()).sendMessage(any(Long.class), any(String.class));

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(notificationLog.getScheduledFor()).isEqualTo(rescheduledFor);

    assertThat(notificationLog.getNextRetryAt()).isEqualTo(rescheduledFor);
  }

  @Test
  void retryStopsWhenDailyReminderIsStale() {
    Instant now = Instant.parse("2026-07-30T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minus(Duration.ofSeconds(1)));

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = currentPlan(cycleRecordId, LocalTime.of(8, 0));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    processor.retry(logId, now);

    verify(telegramBotClient, never()).sendMessage(any(Long.class), any(String.class));

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("Retry stopped because the daily reminder is stale.");
  }

  @Test
  void retryStopsWhenNewCycleRecordExists() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID oldCycleRecordId = UUID.randomUUID();

    UUID newCycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, oldCycleRecordId, now.minus(Duration.ofSeconds(1)));

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = currentPlan(newCycleRecordId, LocalTime.of(8, 0));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    processor.retry(logId, now);

    verify(telegramBotClient, never()).sendMessage(any(Long.class), any(String.class));

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("Retry stopped because a newer cycle record exists.");
  }

  private NotificationLog failedNotificationLog(
      User user, UUID cycleRecordId, Instant nextRetryAt) {
    CycleRecord cycleRecord = CycleRecord.create(user, LocalDate.of(2026, 7, 1));

    ReflectionTestUtils.setField(cycleRecord, "id", cycleRecordId);

    NotificationLog notificationLog =
        NotificationLog.createPending(
            user,
            cycleRecord,
            ChannelType.TELEGRAM,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 7, 29),
            ReminderStage.UPCOMING,
            -3,
            MESSAGE_BODY,
            Instant.parse("2026-07-29T08:00:00Z"));

    notificationLog.startAttempt(Instant.parse("2026-07-29T08:00:00Z"));

    notificationLog.markFailed("previous failure", nextRetryAt);

    return notificationLog;
  }

  private CycleReminderPlan currentPlan(UUID cycleRecordId, LocalTime notificationTime) {
    return new CycleReminderPlan(
        cycleRecordId,
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 7, 29),
        LocalDate.of(2026, 8, 15),
        notificationTime);
  }

  private NotificationChannel connectedTelegramChannel(User user) {
    NotificationChannel channel = NotificationChannel.telegram(user);

    channel.connectTelegram(123456789L, "bebo_user", Instant.parse("2026-07-29T07:00:00Z"));

    return channel;
  }

  private User userWithId(UUID id) {
    User user = User.create("test-" + id + "@example.com", "{noop}secret", "UTC");

    ReflectionTestUtils.setField(user, "id", id);

    return user;
  }
}
