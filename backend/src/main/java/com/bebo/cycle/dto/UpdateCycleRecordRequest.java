package com.bebo.cycle.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateCycleRecordRequest(
    @NotNull(message = "Cycle start date is required") LocalDate startDate) {}
