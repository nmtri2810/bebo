package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bebo.cycle.CycleRecord;
import com.bebo.cycle.CycleRecordRepository;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.NotificationType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CycleReminderProcessorTest {

  private static final String MESSAGE_BODY = "Daily cycle reminder message";

  @Mock private NotificationChannelRepository notificationChannelRepository;

  @Mock private NotificationLogRepository notificationLogRepository;

  @Mock private CycleRecordRepository cycleRecordRepository;

  @Mock private CycleReminderPlanService cycleReminderPlanService;

  @Mock private CycleReminderMessageBuilder messageBuilder;

  @Mock private NotificationRetryPolicy notificationRetryPolicy;

  @Mock private TelegramBotClient telegramBotClient;

  private CycleReminderProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new CycleReminderProcessor(
            notificationChannelRepository,
            notificationLogRepository,
            cycleRecordRepository,
            cycleReminderPlanService,
            messageBuilder,
            notificationRetryPolicy,
            telegramBotClient);
  }

  @Test
  void processSendsDailyTelegramReminderAndMarksLogSent() {
    Instant now = Instant.parse("2026-07-29T08:00:00Z");

    UUID channelId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = dailyPlan(cycleRecordId);

    CycleRecord cycleRecord = CycleRecord.create(user, LocalDate.of(2026, 7, 1));

    when(notificationChannelRepository.findById(channelId)).thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    when(notificationLogRepository
            .existsByUserIdAndPredictedPeriodDateAndDeliveryLocalDateAndNotificationTypeAndChannelType(
                user.getId(),
                plan.predictedPeriodDate(),
                LocalDate.of(2026, 7, 29),
                NotificationType.CYCLE_APPROACHING,
                ChannelType.TELEGRAM))
        .thenReturn(false);

    when(cycleRecordRepository.getReferenceById(cycleRecordId)).thenReturn(cycleRecord);

    when(messageBuilder.build(plan.predictedPeriodDate(), ReminderStage.UPCOMING, -3))
        .thenReturn(MESSAGE_BODY);

    when(notificationLogRepository.saveAndFlush(any(NotificationLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    processor.process(channelId, now);

    ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);

    verify(notificationLogRepository).saveAndFlush(logCaptor.capture());

    verify(telegramBotClient).sendMessage(123456789L, MESSAGE_BODY);

    NotificationLog notificationLog = logCaptor.getValue();

    assertThat(notificationLog.getUser()).isSameAs(user);

    assertThat(notificationLog.getCycleRecord()).isSameAs(cycleRecord);

    assertThat(notificationLog.getChannelType()).isEqualTo(ChannelType.TELEGRAM);

    assertThat(notificationLog.getNotificationType()).isEqualTo(NotificationType.CYCLE_APPROACHING);

    assertThat(notificationLog.getPredictedPeriodDate()).isEqualTo(plan.predictedPeriodDate());

    assertThat(notificationLog.getDeliveryLocalDate()).isEqualTo(LocalDate.of(2026, 7, 29));

    assertThat(notificationLog.getReminderStage()).isEqualTo(ReminderStage.UPCOMING);

    assertThat(notificationLog.getDaysRelativeToPrediction()).isEqualTo(-3);

    assertThat(notificationLog.getMessageBody()).isEqualTo(MESSAGE_BODY);

    assertThat(notificationLog.getScheduledFor()).isEqualTo(now);

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.SENT);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(1);

    assertThat(notificationLog.getLastAttemptAt()).isEqualTo(now);

    assertThat(notificationLog.getSentAt()).isEqualTo(now);

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage()).isNull();
  }

  @Test
  void processMarksDailyLogFailedAndSchedulesRetryWhenTelegramSendFails() {
    Instant now = Instant.parse("2026-07-29T08:00:00Z");

    Instant nextRetryAt = now.plus(Duration.ofMinutes(5));

    UUID channelId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = dailyPlan(cycleRecordId);

    when(notificationChannelRepository.findById(channelId)).thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    when(notificationLogRepository
            .existsByUserIdAndPredictedPeriodDateAndDeliveryLocalDateAndNotificationTypeAndChannelType(
                user.getId(),
                plan.predictedPeriodDate(),
                LocalDate.of(2026, 7, 29),
                NotificationType.CYCLE_APPROACHING,
                ChannelType.TELEGRAM))
        .thenReturn(false);

    when(cycleRecordRepository.getReferenceById(cycleRecordId))
        .thenReturn(CycleRecord.create(user, LocalDate.of(2026, 7, 1)));

    when(messageBuilder.build(plan.predictedPeriodDate(), ReminderStage.UPCOMING, -3))
        .thenReturn(MESSAGE_BODY);

    when(notificationLogRepository.saveAndFlush(any(NotificationLog.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    doThrow(new IllegalStateException("Telegram is down"))
        .when(telegramBotClient)
        .sendMessage(123456789L, MESSAGE_BODY);

    when(notificationRetryPolicy.nextRetryAtAfterFailure(1, now))
        .thenReturn(Optional.of(nextRetryAt));

    processor.process(channelId, now);

    ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);

    verify(notificationLogRepository).saveAndFlush(logCaptor.capture());

    NotificationLog notificationLog = logCaptor.getValue();

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(1);

    assertThat(notificationLog.getLastAttemptAt()).isEqualTo(now);

    assertThat(notificationLog.getSentAt()).isNull();

    assertThat(notificationLog.getNextRetryAt()).isEqualTo(nextRetryAt);

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("IllegalStateException: Telegram is down");
  }

  @Test
  void processDoesNotSendBeforeDailyNotificationTime() {
    Instant now = Instant.parse("2026-07-29T07:59:00Z");

    UUID channelId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationChannel channel = connectedTelegramChannel(user);

    CycleReminderPlan plan = dailyPlan(UUID.randomUUID());

    when(notificationChannelRepository.findById(channelId)).thenReturn(Optional.of(channel));

    when(cycleReminderPlanService.createPlan(user)).thenReturn(Optional.of(plan));

    processor.process(channelId, now);

    verify(notificationLogRepository, never()).saveAndFlush(any(NotificationLog.class));

    verify(telegramBotClient, never()).sendMessage(any(Long.class), any(String.class));
  }

  private CycleReminderPlan dailyPlan(UUID cycleRecordId) {
    return new CycleReminderPlan(
        cycleRecordId,
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 7, 29),
        LocalDate.of(2026, 8, 15),
        LocalTime.of(8, 0));
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
