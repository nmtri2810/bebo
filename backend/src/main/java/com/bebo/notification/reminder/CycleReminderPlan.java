package com.bebo.notification.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

public record CycleReminderPlan(
    UUID latestCycleRecordId,
    LocalDate predictedPeriodDate,
    LocalDate reminderStartDate,
    LocalDate reminderEndDate,
    LocalTime notificationTime) {

  public Optional<Delivery> createDueDelivery(Instant now, ZoneId userZone) {
    LocalDate deliveryLocalDate = now.atZone(userZone).toLocalDate();

    if (!includesDeliveryDate(deliveryLocalDate)) {
      return Optional.empty();
    }

    Instant scheduledFor = scheduledFor(deliveryLocalDate, userZone);

    if (now.isBefore(scheduledFor)) {
      return Optional.empty();
    }

    int daysRelativeToPrediction =
        Math.toIntExact(ChronoUnit.DAYS.between(predictedPeriodDate, deliveryLocalDate));

    ReminderStage stage = ReminderStage.fromDaysRelativeToPrediction(daysRelativeToPrediction);

    return Optional.of(
        new Delivery(deliveryLocalDate, scheduledFor, stage, daysRelativeToPrediction));
  }

  public boolean includesDeliveryDate(LocalDate deliveryLocalDate) {
    return !deliveryLocalDate.isBefore(reminderStartDate)
        && !deliveryLocalDate.isAfter(reminderEndDate);
  }

  public Instant scheduledFor(LocalDate deliveryLocalDate, ZoneId userZone) {
    return ZonedDateTime.of(deliveryLocalDate, notificationTime, userZone).toInstant();
  }

  public record Delivery(
      LocalDate deliveryLocalDate,
      Instant scheduledFor,
      ReminderStage stage,
      int daysRelativeToPrediction) {}
}
