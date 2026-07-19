export type CycleRecord = {
  id: string;
  startDate: string;
  cycleLengthFromPrevious: number | null;
  createdAt: string;
  updatedAt: string;
};

export type CreateCycleRecordRequest = {
  startDate: string;
};

export type CyclePrediction = {
  lastPeriodStartDate: string;
  averageCycleLength: number;
  expectedNextPeriodDate: string;
  reminderDate: string;
  daysRemaining: number;
  predictionSource: "DEFAULT_SETTING" | "AVERAGE_HISTORY";
  historicalCyclesUsed: number;
};

export type UpdateCycleRecordRequest = {
  startDate: string;
};
