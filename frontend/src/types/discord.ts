export type DiscordConnectionStatus = "DISCONNECTED" | "PENDING" | "CONNECTED" | "ALREADY_LINKED";

export type DiscordConnection = {
  status: DiscordConnectionStatus;
  connected: boolean;
  discordUsername: string | null;
  connectedAt: string | null;
};

export type DiscordConnectLink = {
  status: DiscordConnectionStatus;
  authorizationUrl: string;
  expiresAt: string;
};

export type DiscordTestResponse = {
  sent: boolean;
  sentAt: string;
};
