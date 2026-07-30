package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CycleReminderMessageBuilderTest {

  private final CycleReminderMessageBuilder messageBuilder = new CycleReminderMessageBuilder();

  @Test
  void buildCreatesUpcomingCareMessage() {
    String message = messageBuilder.build(LocalDate.of(2026, 8, 2), ReminderStage.UPCOMING, -3);

    assertThat(message)
        .contains("The next period is estimated to start in 3 days.")
        .contains("Estimated start: August 2, 2026")
        .contains("try to be a little more attentive")
        .contains("instead of assuming what they need")
        .contains("daily reminders can stop");
  }

  @Test
  void buildCreatesExpectedTodayCareMessage() {
    String message =
        messageBuilder.build(LocalDate.of(2026, 8, 2), ReminderStage.EXPECTED_TODAY, 0);

    assertThat(message)
        .contains("The next period may start today.")
        .contains("Today may be a good time to check in")
        .contains("anything you can do to help");
  }

  @Test
  void buildCreatesOverdueCareMessage() {
    String message = messageBuilder.build(LocalDate.of(2026, 8, 2), ReminderStage.OVERDUE, 2);

    assertThat(message)
        .contains("The estimated start date passed 2 days ago")
        .contains("a new period has not been logged yet")
        .contains("Cycles can vary")
        .contains("without making assumptions");
  }
}
