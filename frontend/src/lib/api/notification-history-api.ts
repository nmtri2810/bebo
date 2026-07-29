import { apiRequest } from "@/lib/api/api-client";

import type { NotificationHistoryPage } from "@/types/notification-history";

type GetNotificationHistoryParams = {
  page?: number;
  size?: number;
};

export function getNotificationHistory(
  accessToken: string,
  params: GetNotificationHistoryParams = {},
): Promise<NotificationHistoryPage> {
  const page = params.page ?? 0;
  const size = params.size ?? 20;

  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  return apiRequest<NotificationHistoryPage>(`/api/notifications/history?${searchParams.toString()}`, {
    method: "GET",
    token: accessToken,
  });
}
