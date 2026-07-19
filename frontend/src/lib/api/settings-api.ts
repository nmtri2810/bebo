import { apiRequest } from "@/lib/api/api-client";

import type { Settings, UpdateSettingsRequest } from "@/types/settings";

export function getSettings(accessToken: string): Promise<Settings> {
  return apiRequest<Settings>("/api/settings", {
    method: "GET",
    token: accessToken,
  });
}

export function updateSettings(accessToken: string, request: UpdateSettingsRequest): Promise<Settings> {
  return apiRequest<Settings>("/api/settings", {
    method: "PUT",
    token: accessToken,
    body: JSON.stringify(request),
  });
}
