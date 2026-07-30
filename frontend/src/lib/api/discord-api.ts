import { apiRequest } from "@/lib/api/api-client";

import type { DiscordConnectLink, DiscordConnection, DiscordTestResponse } from "@/types/discord";

export function getDiscordConnection(accessToken: string): Promise<DiscordConnection> {
  return apiRequest<DiscordConnection>("/api/notification-channels/discord", {
    method: "GET",
    token: accessToken,
  });
}

export function beginDiscordConnection(accessToken: string): Promise<DiscordConnectLink> {
  return apiRequest<DiscordConnectLink>("/api/notification-channels/discord/connect", {
    method: "POST",
    token: accessToken,
  });
}

export function sendDiscordTest(accessToken: string): Promise<DiscordTestResponse> {
  return apiRequest<DiscordTestResponse>("/api/notification-channels/discord/test", {
    method: "POST",
    token: accessToken,
  });
}

export function disconnectDiscord(accessToken: string): Promise<void> {
  return apiRequest<void>("/api/notification-channels/discord", {
    method: "DELETE",
    token: accessToken,
  });
}
