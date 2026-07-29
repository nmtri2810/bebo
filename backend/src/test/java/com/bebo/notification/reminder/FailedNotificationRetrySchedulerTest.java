package com.bebo.notification.reminder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FailedNotificationRetrySchedulerTest {

  @Mock private NotificationLogRepository notificationLogRepository;

  @Mock private FailedNotificationRetryProcessor retryProcessor;

  private ReminderProperties reminderProperties;

  private FailedNotificationRetryScheduler scheduler;

  @BeforeEach
  void setUp() {
    reminderProperties = new ReminderProperties();
    reminderProperties.setRetryBatchSize(2);

    scheduler =
        new FailedNotificationRetryScheduler(
            notificationLogRepository, retryProcessor, reminderProperties);
  }

  @Test
  void retryFailedNotificationsDoesNothingWhenReminderIsDisabled() {
    reminderProperties.setEnabled(false);

    scheduler.retryFailedNotifications();

    verifyNoInteractions(notificationLogRepository, retryProcessor);
  }

  @Test
  void retryFailedNotificationsProcessesDueFailedNotificationLogs() {
    UUID firstLogId = UUID.randomUUID();
    UUID secondLogId = UUID.randomUUID();

    when(notificationLogRepository.findDueRetryIds(
            eq(NotificationStatus.FAILED),
            eq(ChannelType.TELEGRAM),
            eq(NotificationType.CYCLE_APPROACHING),
            any(Instant.class),
            eq(PageRequest.of(0, 2))))
        .thenReturn(List.of(firstLogId, secondLogId));

    scheduler.retryFailedNotifications();

    verify(retryProcessor).retry(eq(firstLogId), any(Instant.class));
    verify(retryProcessor).retry(eq(secondLogId), any(Instant.class));
  }

  @Test
  void retryFailedNotificationsContinuesWhenOneRetryFails() {
    UUID failingLogId = UUID.randomUUID();
    UUID nextLogId = UUID.randomUUID();

    when(notificationLogRepository.findDueRetryIds(
            eq(NotificationStatus.FAILED),
            eq(ChannelType.TELEGRAM),
            eq(NotificationType.CYCLE_APPROACHING),
            any(Instant.class),
            eq(PageRequest.of(0, 2))))
        .thenReturn(List.of(failingLogId, nextLogId));

    doThrow(new IllegalStateException("boom"))
        .when(retryProcessor)
        .retry(eq(failingLogId), any(Instant.class));

    scheduler.retryFailedNotifications();

    verify(retryProcessor).retry(eq(failingLogId), any(Instant.class));
    verify(retryProcessor).retry(eq(nextLogId), any(Instant.class));
  }

  @Test
  void retryFailedNotificationsSkipsProcessorWhenThereAreNoDueRetries() {
    when(notificationLogRepository.findDueRetryIds(
            eq(NotificationStatus.FAILED),
            eq(ChannelType.TELEGRAM),
            eq(NotificationType.CYCLE_APPROACHING),
            any(Instant.class),
            eq(PageRequest.of(0, 2))))
        .thenReturn(List.of());

    scheduler.retryFailedNotifications();

    verify(retryProcessor, never()).retry(any(UUID.class), any(Instant.class));
  }
}
