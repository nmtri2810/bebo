package com.bebo.cycle.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateCycleRecordRequest(
    @NotNull(message = "Cycle start date is required") LocalDate startDate) {}
