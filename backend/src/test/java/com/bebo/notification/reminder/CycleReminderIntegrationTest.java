package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import com.bebo.cycle.CycleRecord;
import com.bebo.cycle.CycleRecordRepository;
import com.bebo.notification.ChannelType;
import com.bebo.notification.NotificationChannel;
import com.bebo.notification.NotificationChannelRepository;
import com.bebo.notification.NotificationLog;
import com.bebo.notification.NotificationLogRepository;
import com.bebo.notification.NotificationStatus;
import com.bebo.notification.delivery.NotificationDeliveryRequest;
import com.bebo.notification.delivery.NotificationSender;
import com.bebo.settings.CycleSettings;
import com.bebo.settings.CycleSettingsRepository;
import com.bebo.user.User;
import com.bebo.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "bebo.telegram.enabled=false",
      "bebo.discord.enabled=false",
      "bebo.reminder.enabled=false",
      "bebo.reminder.first-retry-delay=PT5M",
      "bebo.reminder.second-retry-delay=PT15M",
      "bebo.reminder.third-retry-delay=PT30M"
    })
@Transactional
class CycleReminderIntegrationTest {

  @Autowired private UserRepository userRepository;

  @Autowired private CycleSettingsRepository cycleSettingsRepository;

  @Autowired private CycleRecordRepository cycleRecordRepository;

  @Autowired private NotificationChannelRepository notificationChannelRepository;

  @Autowired private NotificationLogRepository notificationLogRepository;

  @Autowired private CycleReminderProcessor cycleReminderProcessor;

  @Autowired private FailedNotificationRetryProcessor failedNotificationRetryProcessor;

  @Autowired private RecordingNotificationSender notificationSender;

  @BeforeEach
  void setUp() {
    notificationSender.reset();
  }

  @Test
  void createsSentNotificationLogWhenReminderDeliverySucceeds() {
    User user = createUserWithDefaultSettings();

    CycleRecord cycleRecord =
        cycleRecordRepository.saveAndFlush(CycleRecord.create(user, LocalDate.of(2026, 7, 1)));

    NotificationChannel channel =
        notificationChannelRepository.saveAndFlush(NotificationChannel.telegram(user, "123456789"));

    Instant now = Instant.parse("2026-07-26T08:00:00Z");

    cycleReminderProcessor.process(channel.getId(), now);

    List<NotificationLog> logs = notificationLogsForUser(user);

    assertThat(logs).hasSize(1);

    NotificationLog log = logs.getFirst();

    assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);

    assertThat(log.getCycleRecord().getId()).isEqualTo(cycleRecord.getId());

    assertThat(log.getChannelType()).isEqualTo(ChannelType.TELEGRAM);

    assertThat(log.getPredictedPeriodDate()).isEqualTo(LocalDate.of(2026, 7, 29));

    assertThat(log.getDeliveryLocalDate()).isEqualTo(LocalDate.of(2026, 7, 26));

    assertThat(log.getReminderStage()).isEqualTo(ReminderStage.UPCOMING);

    assertThat(log.getAttemptCount()).isEqualTo(1);

    assertThat(log.getSentAt()).isEqualTo(now);

    assertThat(notificationSender.sentRequests()).hasSize(1);

    assertThat(notificationSender.sentRequests().getFirst().recipientId()).isEqualTo("123456789");

    assertThat(notificationSender.sentRequests().getFirst().messageBody())
        .isEqualTo(log.getMessageBody());
  }

  @Test
  void createsFailedNotificationLogAndRetriesStoredMessage() {
    User user = createUserWithDefaultSettings();

    cycleRecordRepository.saveAndFlush(CycleRecord.create(user, LocalDate.of(2026, 7, 1)));

    NotificationChannel channel =
        notificationChannelRepository.saveAndFlush(NotificationChannel.telegram(user, "123456789"));

    Instant now = Instant.parse("2026-07-26T08:00:00Z");

    notificationSender.failNextSend();

    cycleReminderProcessor.process(channel.getId(), now);

    NotificationLog failedLog = notificationLogsForUser(user).getFirst();

    assertThat(failedLog.getStatus()).isEqualTo(NotificationStatus.FAILED);

    assertThat(failedLog.getAttemptCount()).isEqualTo(1);

    assertThat(failedLog.getNextRetryAt()).isEqualTo(now.plusSeconds(300));

    assertThat(notificationSender.sentRequests()).isEmpty();

    failedNotificationRetryProcessor.retry(failedLog.getId(), failedLog.getNextRetryAt());

    NotificationLog retriedLog = notificationLogRepository.findById(failedLog.getId()).orElseThrow();

    assertThat(retriedLog.getStatus()).isEqualTo(NotificationStatus.SENT);

    assertThat(retriedLog.getAttemptCount()).isEqualTo(2);

    assertThat(retriedLog.getNextRetryAt()).isNull();

    assertThat(notificationSender.sentRequests()).hasSize(1);

    assertThat(notificationSender.sentRequests().getFirst().messageBody())
        .isEqualTo(retriedLog.getMessageBody());
  }

  private User createUserWithDefaultSettings() {
    User user =
        userRepository.saveAndFlush(
            User.create("reminder-it-" + System.nanoTime() + "@example.com", "{noop}secret", "UTC"));

    cycleSettingsRepository.saveAndFlush(CycleSettings.createDefault(user));

    return user;
  }

  private List<NotificationLog> notificationLogsForUser(User user) {
    return notificationLogRepository.findAll().stream()
        .filter(notificationLog -> notificationLog.getUser().getId().equals(user.getId()))
        .toList();
  }

  @TestConfiguration
  static class CycleReminderIntegrationTestConfiguration {

    @Bean
    RecordingNotificationSender recordingNotificationSender() {
      return new RecordingNotificationSender();
    }
  }

  static class RecordingNotificationSender implements NotificationSender {

    private final List<NotificationDeliveryRequest> sentRequests = new ArrayList<>();

    private boolean failNextSend;

    @Override
    public ChannelType supportedChannel() {
      return ChannelType.TELEGRAM;
    }

    @Override
    public void send(NotificationDeliveryRequest request) {
      if (failNextSend) {
        failNextSend = false;

        throw new IllegalStateException("test sender unavailable");
      }

      sentRequests.add(request);
    }

    void failNextSend() {
      failNextSend = true;
    }

    List<NotificationDeliveryRequest> sentRequests() {
      return sentRequests;
    }

    void reset() {
      sentRequests.clear();

      failNextSend = false;
    }
  }
}
