package com.bebo.settings.dto;

import com.bebo.settings.CycleSettings;
import com.bebo.user.User;
import java.time.format.DateTimeFormatter;

public record SettingsResponse(
    int defaultCycleLength, int reminderDaysBefore, String notificationTime, String timezone) {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public static SettingsResponse from(CycleSettings settings, User user) {
    return new SettingsResponse(
        settings.getDefaultCycleLength(),
        settings.getReminderDaysBefore(),
        settings.getNotificationTime().format(TIME_FORMATTER),
        user.getTimezone());
  }
}
