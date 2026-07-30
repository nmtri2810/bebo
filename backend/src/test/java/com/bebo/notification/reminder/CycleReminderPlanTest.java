package com.bebo.notification.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CycleReminderPlanTest {

  private final CycleReminderPlan plan =
      new CycleReminderPlan(
          UUID.randomUUID(),
          LocalDate.of(2026, 8, 2),
          LocalDate.of(2026, 7, 30),
          LocalDate.of(2026, 8, 16),
          LocalTime.of(8, 0));

  @Test
  void createDueDeliveryCreatesUpcomingDailyDelivery() {
    CycleReminderPlan.Delivery delivery =
        plan.createDueDelivery(Instant.parse("2026-07-30T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))
            .orElseThrow();

    assertThat(delivery.deliveryLocalDate()).isEqualTo(LocalDate.of(2026, 7, 30));

    assertThat(delivery.stage()).isEqualTo(ReminderStage.UPCOMING);

    assertThat(delivery.daysRelativeToPrediction()).isEqualTo(-3);
  }

  @Test
  void createDueDeliveryCreatesExpectedTodayDelivery() {
    CycleReminderPlan.Delivery delivery =
        plan.createDueDelivery(Instant.parse("2026-08-02T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))
            .orElseThrow();

    assertThat(delivery.stage()).isEqualTo(ReminderStage.EXPECTED_TODAY);

    assertThat(delivery.daysRelativeToPrediction()).isZero();
  }

  @Test
  void createDueDeliveryCreatesOverdueDelivery() {
    CycleReminderPlan.Delivery delivery =
        plan.createDueDelivery(Instant.parse("2026-08-04T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))
            .orElseThrow();

    assertThat(delivery.stage()).isEqualTo(ReminderStage.OVERDUE);

    assertThat(delivery.daysRelativeToPrediction()).isEqualTo(2);
  }

  @Test
  void createDueDeliveryReturnsEmptyBeforeNotificationTime() {
    assertThat(
            plan.createDueDelivery(
                Instant.parse("2026-07-30T00:59:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")))
        .isEmpty();
  }

  @Test
  void createDueDeliveryReturnsEmptyAfterMaximumOverdueDate() {
    assertThat(
            plan.createDueDelivery(
                Instant.parse("2026-08-17T01:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh")))
        .isEmpty();
  }
}
