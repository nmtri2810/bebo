package com.bebo.cycle;

import static org.assertj.core.api.Assertions.assertThat;

import com.bebo.cycle.dto.PredictionSource;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CyclePredictionCalculatorTest {

  @Test
  void shouldUseDefaultWhenOnlyOneRecordExists() {
    List<LocalDate> dates = List.of(LocalDate.of(2026, 7, 1));

    CyclePredictionCalculator.Calculation result =
        CyclePredictionCalculator.calculateAverage(dates, 28);

    assertThat(result.averageCycleLength()).isEqualTo(28);

    assertThat(result.source()).isEqualTo(PredictionSource.DEFAULT_SETTING);

    assertThat(result.historicalCyclesUsed()).isZero();
  }

  @Test
  void shouldCalculateAverageFromHistory() {
    List<LocalDate> dates =
        List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 2), LocalDate.of(2026, 5, 5));

    /*
     * 05/05 → 02/06 = 28
     * 02/06 → 01/07 = 29
     * Average = 28.5
     * Rounded = 29
     */
    CyclePredictionCalculator.Calculation result =
        CyclePredictionCalculator.calculateAverage(dates, 28);

    assertThat(result.averageCycleLength()).isEqualTo(29);

    assertThat(result.source()).isEqualTo(PredictionSource.AVERAGE_HISTORY);

    assertThat(result.historicalCyclesUsed()).isEqualTo(2);
  }

  @Test
  void shouldIgnoreInvalidIntervals() {
    List<LocalDate> dates =
        List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 2), LocalDate.of(2026, 1, 1));

    /*
     * 02/06 → 01/07 = 29 ngày: hợp lệ.
     * 01/01 → 02/06: vượt 60 ngày, bỏ qua.
     */
    CyclePredictionCalculator.Calculation result =
        CyclePredictionCalculator.calculateAverage(dates, 28);

    assertThat(result.averageCycleLength()).isEqualTo(29);

    assertThat(result.historicalCyclesUsed()).isEqualTo(1);
  }
}
