package com.bebo.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record OnboardingCycleRequest(
    @NotNull(message = "Period start date is required") LocalDate startDate,
    @NotNull(message = "Default cycle length is required")
        @Min(value = 15, message = "Default cycle length must be at least 15 days")
        @Max(value = 60, message = "Default cycle length must not exceed 60 days")
        Integer defaultCycleLength) {}
