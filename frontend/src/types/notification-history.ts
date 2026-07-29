export type NotificationChannelType = "TELEGRAM";

export type NotificationType = "CYCLE_APPROACHING";

export type NotificationHistoryStatus = "SENT" | "FAILED";

export type NotificationHistoryItem = {
  id: string;
  channelType: NotificationChannelType;
  notificationType: NotificationType;
  predictedPeriodDate: string;
  scheduledFor: string;
  sentAt: string | null;
  status: NotificationHistoryStatus;
  attemptCount: number;
  lastAttemptAt: string | null;
  nextRetryAt: string | null;
  failureMessage: string | null;
};

export type NotificationHistoryPage = {
  items: NotificationHistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};
