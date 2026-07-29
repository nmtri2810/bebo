package com.bebo.notification.reminder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;

public record CycleReminderPlan(
    UUID latestCycleRecordId,
    LocalDate predictedPeriodDate,
    LocalDate reminderDate,
    Instant scheduledFor,
    int reminderDaysBefore) {

  private static final DateTimeFormatter MESSAGE_DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ENGLISH);

  public boolean isDueAt(Instant now, ZoneId userZone) {
    LocalDate localDate = now.atZone(userZone).toLocalDate();

    return localDate.equals(reminderDate) && !now.isBefore(scheduledFor);
  }

  public String buildMessage() {
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
}
