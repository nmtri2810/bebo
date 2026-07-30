package com.bebo.notification.reminder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CycleReminderMessageBuilder {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ENGLISH);

  public String build(
      LocalDate predictedPeriodDate, ReminderStage stage, int daysRelativeToPrediction) {
    String timingMessage = buildTimingMessage(stage, daysRelativeToPrediction);

    String careMessage = buildCareMessage(stage);

    return """
    bebo cycle reminder

    %s
    Estimated start: %s

    %s

    This is only an estimate. Log the new period in bebo when it starts so daily reminders can stop and the next estimate can update.
    """
        .formatted(timingMessage, predictedPeriodDate.format(DATE_FORMATTER), careMessage)
        .strip();
  }

  private String buildTimingMessage(ReminderStage stage, int daysRelativeToPrediction) {
    return switch (stage) {
      case UPCOMING -> {
        int daysUntilPrediction = Math.abs(daysRelativeToPrediction);

        if (daysUntilPrediction == 1) {
          yield "The next period is estimated to start tomorrow.";
        }

        yield "The next period is estimated to start in " + daysUntilPrediction + " days.";
      }

      case EXPECTED_TODAY -> "The next period may start today.";

      case OVERDUE -> {
        if (daysRelativeToPrediction == 1) {
          yield "The estimated start date passed 1 day ago, "
              + "and a new period has not been logged yet.";
        }

        yield "The estimated start date passed "
            + daysRelativeToPrediction
            + " days ago, and a new period has not been logged yet.";
      }
    };
  }

  private String buildCareMessage(ReminderStage stage) {
    return switch (stage) {
      case UPCOMING ->
          "Over the next few days, try to be a little more attentive. "
              + "Check in, ask how your partner is feeling, and offer help "
              + "instead of assuming what they need.";

      case EXPECTED_TODAY ->
          "Today may be a good time to check in and be a little more attentive. "
              + "Ask how your partner is feeling and whether there is anything "
              + "you can do to help.";

      case OVERDUE ->
          "Cycles can vary, so keep checking in without making assumptions. "
              + "A thoughtful question or a small act of support can still mean a lot.";
    };
  }
}
