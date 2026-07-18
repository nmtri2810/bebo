package com.bebo.cycle;

import com.bebo.cycle.dto.PredictionSource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class CyclePredictionCalculator {

  private static final int MIN_VALID_CYCLE_LENGTH = 15;
  private static final int MAX_VALID_CYCLE_LENGTH = 60;
  private static final int MAX_INTERVALS = 6;

  private CyclePredictionCalculator() {}

  public static Calculation calculateAverage(
      List<LocalDate> startDatesDescending, int defaultCycleLength) {
    if (startDatesDescending == null || startDatesDescending.size() < 2) {
      return new Calculation(defaultCycleLength, PredictionSource.DEFAULT_SETTING, 0);
    }

    List<Long> validIntervals = new ArrayList<>();

    int numberOfIntervals = Math.min(startDatesDescending.size() - 1, MAX_INTERVALS);

    for (int index = 0; index < numberOfIntervals; index++) {
      LocalDate newerDate = startDatesDescending.get(index);

      LocalDate olderDate = startDatesDescending.get(index + 1);

      long interval = ChronoUnit.DAYS.between(olderDate, newerDate);

      /*
       * Bỏ qua những khoảng quá ngắn hoặc quá dài.
       *
       * Việc này tránh một record nhập sai làm méo
       * toàn bộ prediction.
       */
      if (interval >= MIN_VALID_CYCLE_LENGTH && interval <= MAX_VALID_CYCLE_LENGTH) {
        validIntervals.add(interval);
      }
    }

    if (validIntervals.isEmpty()) {
      return new Calculation(defaultCycleLength, PredictionSource.DEFAULT_SETTING, 0);
    }

    double average =
        validIntervals.stream().mapToLong(Long::longValue).average().orElse(defaultCycleLength);

    int roundedAverage = (int) Math.round(average);

    return new Calculation(roundedAverage, PredictionSource.AVERAGE_HISTORY, validIntervals.size());
  }

  public record Calculation(
      int averageCycleLength, PredictionSource source, int historicalCyclesUsed) {}
}
