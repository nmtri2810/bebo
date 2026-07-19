package com.bebo.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
    @NotNull(message = "Default cycle length is required")
        @Min(value = 15, message = "Default cycle length must be at least 15 days")
        @Max(value = 60, message = "Default cycle length must not exceed 60 days")
        Integer defaultCycleLength,
    @NotNull(message = "Reminder days is required")
        @Min(value = 0, message = "Reminder days cannot be negative")
        @Max(value = 14, message = "Reminder days must not exceed 14")
        Integer reminderDaysBefore,
    @NotBlank(message = "Notification time is required")
        @Pattern(
            regexp = "^([01]\\d|2[0-3]):[0-5]\\d$",
            message = "Notification time must use HH:mm format")
        String notificationTime,
    @NotBlank(message = "Timezone is required")
        @Size(max = 100, message = "Timezone must not exceed 100 characters")
        String timezone) {}
