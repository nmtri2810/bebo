package com.bebo.user.dto;

import com.bebo.notification.NotificationChannelStatus;
import com.bebo.user.OnboardingStep;
import java.time.Instant;
import java.time.LocalDate;

public record OnboardingStateResponse(
    OnboardingStep step,
    Instant completedAt,
    LocalDate mostRecentPeriodStartDate,
    int defaultCycleLength,
    int reminderDaysBefore,
    String notificationTime,
    String timezone,
    NotificationChannelStatus telegramStatus,
    boolean telegramConnected,
    String telegramUsername,
    LocalDate expectedNextPeriodDate,
    LocalDate reminderDate) {}
