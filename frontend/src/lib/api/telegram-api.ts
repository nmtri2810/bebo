import { apiRequest } from "@/lib/api/api-client";

import type { TelegramConnectLink, TelegramConnection } from "@/types/telegram";

export function getTelegramConnection(accessToken: string): Promise<TelegramConnection> {
  return apiRequest<TelegramConnection>("/api/notification-channels/telegram", {
    method: "GET",
    token: accessToken,
  });
}

export function beginTelegramConnection(accessToken: string): Promise<TelegramConnectLink> {
  return apiRequest<TelegramConnectLink>("/api/notification-channels/telegram/connect", {
    method: "POST",
    token: accessToken,
  });
}

export function disconnectTelegram(accessToken: string): Promise<void> {
  return apiRequest<void>("/api/notification-channels/telegram", {
    method: "DELETE",
    token: accessToken,
  });
}
