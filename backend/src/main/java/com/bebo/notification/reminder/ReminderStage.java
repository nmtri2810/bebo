package com.bebo.notification.reminder;

public enum ReminderStage {
  UPCOMING,
  EXPECTED_TODAY,
  OVERDUE;

  public static ReminderStage fromDaysRelativeToPrediction(int daysRelativeToPrediction) {
    if (daysRelativeToPrediction < 0) {
      return UPCOMING;
    }

    if (daysRelativeToPrediction == 0) {
      return EXPECTED_TODAY;
    }

    return OVERDUE;
  }
}
