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
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationDispatcher;
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
class FailedNotificationRetryProcessorTest {

  private static final String MESSAGE_BODY = "Stored daily reminder message";

  @Mock private NotificationLogRepository notificationLogRepository;

  @Mock private NotificationChannelRepository notificationChannelRepository;

  @Mock private CycleReminderPlanService cycleReminderPlanService;

  @Mock private NotificationRetryPolicy notificationRetryPolicy;

  @Mock private NotificationDispatcher notificationDispatcher;

  private FailedNotificationRetryProcessor processor;

  @BeforeEach
  void setUp() {
    processor =
        new FailedNotificationRetryProcessor(
            notificationLogRepository,
            notificationChannelRepository,
            cycleReminderPlanService,
            notificationRetryPolicy,
            notificationDispatcher);
  }

  @Test
  void retryDispatchesStoredMessageThroughOriginalChannel() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minusSeconds(1));

    NotificationChannel channel = connectedTelegramChannel(user);

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(notificationDispatcher.supports(ChannelType.TELEGRAM)).thenReturn(true);

    when(cycleReminderPlanService.createPlan(user))
        .thenReturn(Optional.of(currentPlan(cycleRecordId, LocalTime.of(8, 0))));

    processor.retry(logId, now);

    ArgumentCaptor<NotificationDeliveryRequest> requestCaptor =
        ArgumentCaptor.forClass(NotificationDeliveryRequest.class);

    verify(notificationDispatcher)
        .send(org.mockito.ArgumentMatchers.eq(ChannelType.TELEGRAM), requestCaptor.capture());

    assertThat(requestCaptor.getValue().recipientId()).isEqualTo("123456789");

    assertThat(requestCaptor.getValue().messageBody()).isEqualTo(MESSAGE_BODY);

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.SENT);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(2);

    assertThat(notificationLog.getSentAt()).isEqualTo(now);
  }

  @Test
  void retrySchedulesNextAttemptWhenSenderFails() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    Instant nextRetryAt = now.plus(Duration.ofMinutes(15));

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minusSeconds(1));

    NotificationChannel channel = connectedTelegramChannel(user);

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(notificationDispatcher.supports(ChannelType.TELEGRAM)).thenReturn(true);

    when(cycleReminderPlanService.createPlan(user))
        .thenReturn(Optional.of(currentPlan(cycleRecordId, LocalTime.of(8, 0))));

    doThrow(new IllegalStateException("sender unavailable"))
        .when(notificationDispatcher)
        .send(ChannelType.TELEGRAM, new NotificationDeliveryRequest("123456789", MESSAGE_BODY));

    when(notificationRetryPolicy.nextRetryAtAfterFailure(2, now))
        .thenReturn(Optional.of(nextRetryAt));

    processor.retry(logId, now);

    assertThat(notificationLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(notificationLog.getAttemptCount()).isEqualTo(2);

    assertThat(notificationLog.getNextRetryAt()).isEqualTo(nextRetryAt);

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("IllegalStateException: sender unavailable");
  }

  @Test
  void retryStopsWhenChannelIsUnavailable() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minusSeconds(1));

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.empty());

    processor.retry(logId, now);

    verify(notificationDispatcher, never())
        .send(any(ChannelType.class), any(NotificationDeliveryRequest.class));

    assertThat(notificationLog.getNextRetryAt()).isNull();

    assertThat(notificationLog.getErrorMessage())
        .isEqualTo("Retry stopped because the notification channel is disconnected or disabled.");
  }

  @Test
  void retryKeepsRetryWhenOriginalChannelHasNoSender() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID cycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, cycleRecordId, now.minusSeconds(1));

    NotificationChannel channel = connectedTelegramChannel(user);

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(notificationDispatcher.supports(ChannelType.TELEGRAM)).thenReturn(false);

    Instant existingRetryAt = notificationLog.getNextRetryAt();

    processor.retry(logId, now);

    verify(cycleReminderPlanService, never()).createPlan(any(User.class));

    verify(notificationDispatcher, never())
        .send(any(ChannelType.class), any(NotificationDeliveryRequest.class));

    assertThat(notificationLog.getNextRetryAt()).isEqualTo(existingRetryAt);
  }

  @Test
  void retryStopsWhenNewCycleRecordExists() {
    Instant now = Instant.parse("2026-07-29T08:05:00Z");

    UUID logId = UUID.randomUUID();

    UUID oldCycleRecordId = UUID.randomUUID();

    UUID newCycleRecordId = UUID.randomUUID();

    User user = userWithId(UUID.randomUUID());

    NotificationLog notificationLog =
        failedNotificationLog(user, oldCycleRecordId, now.minusSeconds(1));

    NotificationChannel channel = connectedTelegramChannel(user);

    when(notificationLogRepository.findByIdForUpdate(logId))
        .thenReturn(Optional.of(notificationLog));

    when(notificationChannelRepository.findByUser_IdAndChannelType(
            user.getId(), ChannelType.TELEGRAM))
        .thenReturn(Optional.of(channel));

    when(notificationDispatcher.supports(ChannelType.TELEGRAM)).thenReturn(true);

    when(cycleReminderPlanService.createPlan(user))
        .thenReturn(Optional.of(currentPlan(newCycleRecordId, LocalTime.of(8, 0))));

    processor.retry(logId, now);

    verify(notificationDispatcher, never())
        .send(any(ChannelType.class), any(NotificationDeliveryRequest.class));

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
