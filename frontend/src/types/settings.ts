export type Settings = {
  defaultCycleLength: number;
  reminderDaysBefore: number;
  notificationTime: string;
  timezone: string;
};

export type UpdateSettingsRequest = {
  defaultCycleLength: number;
  reminderDaysBefore: number;
  notificationTime: string;
  timezone: string;
};
