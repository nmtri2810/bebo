package com.bebo.cycle.dto;

public enum PredictionSource {

  /*
   * Chưa có đủ lịch sử.
   * Dùng defaultCycleLength trong settings.
   */
  DEFAULT_SETTING,

  /*
   * Tính từ các chu kỳ đã nhập.
   */
  AVERAGE_HISTORY
}
