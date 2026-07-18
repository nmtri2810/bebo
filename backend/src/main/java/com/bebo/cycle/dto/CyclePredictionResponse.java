package com.bebo.cycle.dto;

import java.time.LocalDate;

public record CyclePredictionResponse(
    LocalDate lastPeriodStartDate,
    int averageCycleLength,
    LocalDate expectedNextPeriodDate,
    LocalDate reminderDate,
    long daysRemaining,
    PredictionSource predictionSource,
    int historicalCyclesUsed) {}
