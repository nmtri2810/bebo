package com.bebo.cycle.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CycleRecordResponse(
    UUID id,
    LocalDate startDate,

    /*
     * Khoảng cách từ kỳ liền trước đến kỳ này.
     *
     * Ví dụ:
     * 01/07 → 28/07 = 27 ngày.
     *
     * Record cũ nhất sẽ có giá trị null.
     */
    Long cycleLengthFromPrevious,
    Instant createdAt,
    Instant updatedAt) {}
