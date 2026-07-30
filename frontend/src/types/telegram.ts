export type TelegramConnectionStatus = "DISCONNECTED" | "PENDING" | "CONNECTED" | "ALREADY_LINKED";

export type TelegramConnection = {
  status: TelegramConnectionStatus;
  connected: boolean;
  telegramUsername: string | null;
  connectedAt: string | null;
};

export type TelegramConnectLink = {
  status: TelegramConnectionStatus;
  deepLink: string;
  expiresAt: string;
};

export type TelegramTestResponse = {
  sent: boolean;
  sentAt: string;
};
