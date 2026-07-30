package com.bebo.notification.reminder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationChannelStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CycleReminderSchedulerTest {

  @Mock private NotificationChannelRepository notificationChannelRepository;

  @Mock private CycleReminderProcessor cycleReminderProcessor;

  private ReminderProperties reminderProperties;

  private CycleReminderScheduler scheduler;

  @BeforeEach
  void setUp() {
    reminderProperties = new ReminderProperties();

    scheduler =
        new CycleReminderScheduler(
            notificationChannelRepository, cycleReminderProcessor, reminderProperties);
  }

  @Test
  void sendDueRemindersDoesNothingWhenReminderIsDisabled() {
    reminderProperties.setEnabled(false);

    scheduler.sendDueReminders();

    verifyNoInteractions(notificationChannelRepository, cycleReminderProcessor);
  }

  @Test
  void sendDueRemindersProcessesAllConnectedChannels() {
    UUID firstChannelId = UUID.randomUUID();

    UUID secondChannelId = UUID.randomUUID();

    when(notificationChannelRepository.findAllConnectedChannelIds(
            NotificationChannelStatus.CONNECTED))
        .thenReturn(List.of(firstChannelId, secondChannelId));

    scheduler.sendDueReminders();

    verify(cycleReminderProcessor).process(eq(firstChannelId), any(Instant.class));

    verify(cycleReminderProcessor).process(eq(secondChannelId), any(Instant.class));
  }

  @Test
  void sendDueRemindersContinuesWhenOneChannelFails() {
    UUID failingChannelId = UUID.randomUUID();

    UUID nextChannelId = UUID.randomUUID();

    when(notificationChannelRepository.findAllConnectedChannelIds(
            NotificationChannelStatus.CONNECTED))
        .thenReturn(List.of(failingChannelId, nextChannelId));

    doThrow(new IllegalStateException("boom"))
        .when(cycleReminderProcessor)
        .process(eq(failingChannelId), any(Instant.class));

    scheduler.sendDueReminders();

    verify(cycleReminderProcessor).process(eq(failingChannelId), any(Instant.class));

    verify(cycleReminderProcessor).process(eq(nextChannelId), any(Instant.class));
  }

  @Test
  void sendDueRemindersSkipsProcessorWhenThereAreNoConnectedChannels() {
    when(notificationChannelRepository.findAllConnectedChannelIds(
            NotificationChannelStatus.CONNECTED))
        .thenReturn(List.of());

    scheduler.sendDueReminders();

    verify(cycleReminderProcessor, never()).process(any(UUID.class), any(Instant.class));
  }
}
