"use client";

import { type ReactNode, useCallback, useEffect, useState } from "react";

import {
  Bell,
  BellOff,
  BellRing,
  CalendarDays,
  ChevronRight,
  CircleAlert,
  Clock3,
  Gamepad2,
  HeartPulse,
  RefreshCw,
  Send,
} from "lucide-react";

import Link from "next/link";

import { useLocale, useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { AppShell } from "@/components/app-shell";

import { AddCycleDialog } from "@/features/cycles/components/add-cycle-dialog";

import { ManageCycleDialog } from "@/features/cycles/components/manage-cycle-dialog";

import { ApiClientError } from "@/lib/api/api-client";

import { getCycleHistory, getCyclePrediction } from "@/lib/api/cycle-api";

import { getDiscordConnection } from "@/lib/api/discord-api";

import { getNotificationHistory } from "@/lib/api/notification-history-api";

import { getSettings } from "@/lib/api/settings-api";

import { getTelegramConnection } from "@/lib/api/telegram-api";

import { getCurrentUser } from "@/lib/api/user-api";

import { useAuthStore } from "@/stores/auth-store";

import type { AuthUser } from "@/types/auth";

import type { CyclePrediction, CycleRecord } from "@/types/cycle";

import type { DiscordConnection } from "@/types/discord";

import type { NotificationChannelType, NotificationHistoryItem } from "@/types/notification-history";

import type { Settings } from "@/types/settings";

import type { TelegramConnection } from "@/types/telegram";

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);

  return new Date(year, month - 1, day);
}

function formatDate(value: string, locale: string, style: "long" | "short" = "long"): string {
  return new Intl.DateTimeFormat(
    locale === "vi" ? "vi-VN" : "en-US",
    style === "long"
      ? {
          month: "long",
          day: "numeric",
          year: "numeric",
        }
      : {
          month: "short",
          day: "numeric",
          year: "numeric",
        },
  ).format(parseLocalDate(value));
}

function formatLargeDate(
  value: string,
  locale: string,
): {
  month: string;
  day: string;
} {
  const date = parseLocalDate(value);

  const formatterLocale = locale === "vi" ? "vi-VN" : "en-US";

  return {
    month: new Intl.DateTimeFormat(formatterLocale, {
      month: "long",
    }).format(date),

    day: new Intl.DateTimeFormat(formatterLocale, {
      day: "numeric",
    }).format(date),
  };
}

function formatTime(value: string, locale: string): string {
  const [hour, minute] = value.split(":").map(Number);

  if (!Number.isInteger(hour) || !Number.isInteger(minute)) {
    return value;
  }

  const date = new Date(2000, 0, 1, hour, minute);

  return new Intl.DateTimeFormat(locale === "vi" ? "vi-VN" : "en-US", {
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

function formatInstant(value: string, locale: string, timezone: string): string {
  const formatterLocale = locale === "vi" ? "vi-VN" : "en-US";

  try {
    return new Intl.DateTimeFormat(formatterLocale, {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(value));
  } catch {
    return new Intl.DateTimeFormat(formatterLocale, {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: "UTC",
    }).format(new Date(value));
  }
}

function getSettledValue<T>(result: PromiseSettledResult<T>): T | null {
  if (result.status === "fulfilled") {
    return result.value;
  }

  if (result.reason instanceof ApiClientError && result.reason.status === 401) {
    throw result.reason;
  }

  return null;
}

type LatestNotificationMap = {
  TELEGRAM: NotificationHistoryItem | null;

  DISCORD: NotificationHistoryItem | null;
};

type DashboardData = {
  currentUser: AuthUser;
  history: CycleRecord[];
  prediction: CyclePrediction | null;
  settings: Settings | null;

  telegramConnection: TelegramConnection | null;

  discordConnection: DiscordConnection | null;

  latestNotifications: LatestNotificationMap;
};

function findLatestNotification(
  items: NotificationHistoryItem[],
  channelType: NotificationChannelType,
): NotificationHistoryItem | null {
  return items.find((item) => item.channelType === channelType) ?? null;
}

async function fetchDashboardData(accessToken: string): Promise<DashboardData> {
  const [currentUser, history] = await Promise.all([getCurrentUser(accessToken), getCycleHistory(accessToken)]);

  if (history.length === 0) {
    return {
      currentUser,
      history,
      prediction: null,
      settings: null,
      telegramConnection: null,
      discordConnection: null,

      latestNotifications: {
        TELEGRAM: null,
        DISCORD: null,
      },
    };
  }

  const [predictionResult, settingsResult, telegramResult, discordResult, notificationHistoryResult] =
    await Promise.allSettled([
      getCyclePrediction(accessToken),

      getSettings(accessToken),

      getTelegramConnection(accessToken),

      getDiscordConnection(accessToken),

      getNotificationHistory(accessToken, {
        page: 0,
        size: 50,
      }),
    ]);

  const prediction = getSettledValue(predictionResult);

  const settings = getSettledValue(settingsResult);

  const telegramConnection = getSettledValue(telegramResult);

  const discordConnection = getSettledValue(discordResult);

  const notificationHistory = getSettledValue(notificationHistoryResult);

  const notificationItems = notificationHistory?.items ?? [];

  return {
    currentUser,
    history,
    prediction,
    settings,
    telegramConnection,
    discordConnection,

    latestNotifications: {
      TELEGRAM: findLatestNotification(notificationItems, "TELEGRAM"),

      DISCORD: findLatestNotification(notificationItems, "DISCORD"),
    },
  };
}

export default function DashboardPage() {
  const router = useRouter();

  const locale = useLocale();

  const t = useTranslations("Dashboard");

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const setUser = useAuthStore((state) => state.setUser);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [history, setHistory] = useState<CycleRecord[]>([]);

  const [prediction, setPrediction] = useState<CyclePrediction | null>(null);

  const [settings, setSettings] = useState<Settings | null>(null);

  const [telegramConnection, setTelegramConnection] = useState<TelegramConnection | null>(null);

  const [discordConnection, setDiscordConnection] = useState<DiscordConnection | null>(null);

  const [latestNotifications, setLatestNotifications] = useState<LatestNotificationMap>({
    TELEGRAM: null,
    DISCORD: null,
  });

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const applyDashboardData = useCallback(
    (data: DashboardData) => {
      setUser(data.currentUser);

      setHistory(data.history);

      setPrediction(data.prediction);

      setSettings(data.settings);

      setTelegramConnection(data.telegramConnection);

      setDiscordConnection(data.discordConnection);

      setLatestNotifications(data.latestNotifications);

      setErrorMessage(null);
    },
    [setUser],
  );

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    fetchDashboardData(accessToken)
      .then((data) => {
        if (cancelled) {
          return;
        }

        if (data.currentUser.onboardingStep !== "COMPLETED" || data.history.length === 0) {
          setUser(data.currentUser);

          router.replace("/onboarding");

          return;
        }

        applyDashboardData(data);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        if (error instanceof ApiClientError && error.status === 401) {
          clearSession();
          router.replace("/");

          return;
        }

        setErrorMessage(t("loadError"));
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, applyDashboardData, clearSession, hasHydrated, router, setUser, t]);

  const handleSessionExpired = useCallback(() => {
    clearSession();
    router.replace("/");
  }, [clearSession, router]);

  const refreshDashboard = useCallback(async () => {
    if (!accessToken) {
      return;
    }

    try {
      const data = await fetchDashboardData(accessToken);

      if (data.currentUser.onboardingStep !== "COMPLETED" || data.history.length === 0) {
        setUser(data.currentUser);

        router.replace("/onboarding");

        return;
      }

      applyDashboardData(data);
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        clearSession();
        router.replace("/");

        return;
      }

      setErrorMessage(t("loadError"));

      throw error;
    }
  }, [accessToken, applyDashboardData, clearSession, router, setUser, t]);

  if (!hasHydrated || isLoading) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <div className="flex flex-col items-center gap-3">
          <div className="size-6 animate-spin rounded-full border-2 border-[#007aff]/20 border-t-[#007aff]" />

          <p className="text-sm text-[#8e8e93]">{t("loading")}</p>
        </div>
      </main>
    );
  }

  if (!accessToken) {
    return null;
  }

  const latestRecord = history[0];

  const predictedDate = prediction ? formatLargeDate(prediction.expectedNextPeriodDate, locale) : null;

  const remainingDays = prediction?.daysRemaining ?? 0;

  return (
    <AppShell maxWidthClassName="max-w-130">
      <div className="mb-5 flex items-end justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-[30px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e]">{t("greeting")}</h1>

          <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{t("description")}</p>

          {user?.email && <p className="mt-1 max-w-75 truncate text-xs text-[#aeaeb2]">{user.email}</p>}
        </div>

        <AddCycleDialog accessToken={accessToken} onCreated={refreshDashboard} />
      </div>

      {errorMessage && (
        <div className="mb-5 rounded-[16px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">{errorMessage}</div>
      )}

      <section className="overflow-hidden rounded-[28px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] p-6 text-white shadow-[0_14px_35px_rgba(255,45,85,0.22)]">
        <div className="flex items-center gap-2">
          <CalendarDays className="size-5" />

          <p className="text-sm font-semibold text-white/90">{t("nextPeriod")}</p>
        </div>

        {prediction && predictedDate ? (
          <>
            <div className="mt-7">
              <p className="text-sm font-medium capitalize text-white/75">{predictedDate.month}</p>

              <p className="text-[68px] font-bold leading-[0.95] tracking-[-0.06em]">{predictedDate.day}</p>
            </div>

            <p className="mt-5 text-[17px] font-semibold">
              {remainingDays >= 0
                ? t("daysUntil", {
                    count: remainingDays,
                  })
                : t("daysPast", {
                    count: Math.abs(remainingDays),
                  })}
            </p>

            <p className="mt-1 text-sm text-white/75">
              {prediction.predictionSource === "AVERAGE_HISTORY"
                ? t("basedOnHistory", {
                    count: prediction.historicalCyclesUsed,
                  })
                : t("basedOnDefault", {
                    days: prediction.averageCycleLength,
                  })}
            </p>
          </>
        ) : (
          <p className="mt-8 text-sm text-white/80">{t("predictionUnavailable")}</p>
        )}
      </section>

      <ReminderStatusCard
        prediction={prediction}
        settings={settings}
        telegramConnection={telegramConnection}
        discordConnection={discordConnection}
        latestNotifications={latestNotifications}
        locale={locale}
      />

      {prediction && latestRecord && (
        <section className="mt-5 overflow-hidden rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
          <InfoRow
            icon={<CalendarDays className="size-5 text-[#ff2d55]" />}
            label={t("latestPeriod")}
            value={formatDate(latestRecord.startDate, locale, "short")}
          />

          <div className="ml-14 border-t border-black/6" />

          <InfoRow
            icon={<HeartPulse className="size-5 text-[#af52de]" />}
            label={t("averageCycle")}
            value={t("averageCycleValue", {
              days: prediction.averageCycleLength,
            })}
          />

          <div className="ml-14 border-t border-black/6" />

          <InfoRow
            icon={<Bell className="size-5 text-[#007aff]" />}
            label={t("reminderDate")}
            value={formatDate(prediction.reminderDate, locale, "short")}
          />
        </section>
      )}

      <section className="mt-8">
        <h2 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">
          {t("recentHistory")}
        </h2>

        <div className="overflow-hidden rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
          {history.slice(0, 6).map((record, index) => {
            const secondaryText = record.cycleLengthFromPrevious
              ? t("cycleLength", {
                  days: record.cycleLengthFromPrevious,
                })
              : t("firstRecord");

            return (
              <div key={record.id}>
                {index > 0 && <div className="ml-4 border-t border-black/6" />}

                <ManageCycleDialog
                  accessToken={accessToken}
                  record={record}
                  displayDate={formatDate(record.startDate, locale, "long")}
                  secondaryText={secondaryText}
                  onChanged={refreshDashboard}
                  onUnauthorized={handleSessionExpired}
                />
              </div>
            );
          })}
        </div>
      </section>
    </AppShell>
  );
}

type ReminderStatusCardProps = {
  prediction: CyclePrediction | null;

  settings: Settings | null;

  telegramConnection: TelegramConnection | null;

  discordConnection: DiscordConnection | null;

  latestNotifications: LatestNotificationMap;

  locale: string;
};

type ChannelConnectionStatus = "UNAVAILABLE" | "DISCONNECTED" | "PENDING" | "CONNECTED" | "ALREADY_LINKED";

type ReminderChannelView = {
  channelType: NotificationChannelType;

  label: string;

  status: ChannelConnectionStatus;

  connected: boolean;

  username: string | null;

  latestNotification: NotificationHistoryItem | null;
};

function ReminderStatusCard({
  prediction,
  settings,
  telegramConnection,
  discordConnection,
  latestNotifications,
  locale,
}: ReminderStatusCardProps) {
  const t = useTranslations("Dashboard");

  const timezone = settings?.timezone ?? "UTC";

  const channels: ReminderChannelView[] = [
    {
      channelType: "TELEGRAM",

      label: t("telegram"),

      status: telegramConnection?.status ?? "UNAVAILABLE",

      connected: telegramConnection?.connected ?? false,

      username: telegramConnection?.telegramUsername ?? null,

      latestNotification: latestNotifications.TELEGRAM,
    },
    {
      channelType: "DISCORD",

      label: t("discord"),

      status: discordConnection?.status ?? "UNAVAILABLE",

      connected: discordConnection?.connected ?? false,

      username: discordConnection?.discordUsername ?? null,

      latestNotification: latestNotifications.DISCORD,
    },
  ];

  const availableChannels = channels.filter((channel) => channel.status !== "UNAVAILABLE");

  const connectedChannels = channels.filter((channel) => channel.connected);

  const pendingChannels = channels.filter((channel) => channel.status === "PENDING");

  const failedWithRetry = connectedChannels.find(
    (channel) => channel.latestNotification?.status === "FAILED" && channel.latestNotification.nextRetryAt,
  );

  const failedWithoutRetry = connectedChannels.find(
    (channel) => channel.latestNotification?.status === "FAILED" && !channel.latestNotification.nextRetryAt,
  );

  const nextSchedule =
    prediction && settings
      ? [
          formatDate(prediction.reminderDate, locale, "long"),

          formatTime(settings.notificationTime, locale),

          settings.timezone,
        ].join(" · ")
      : null;

  let summaryIcon: ReactNode;

  let summaryIconClassName: string;

  let badge: string;

  let badgeClassName: string;

  let title: string;

  let description: string;

  if (availableChannels.length === 0) {
    summaryIcon = <Bell className="size-6 text-[#636366]" />;

    summaryIconClassName = "bg-[#8e8e93]/10";

    badge = t("statusUnavailable");

    badgeClassName = "bg-[#8e8e93]/10 text-[#636366]";

    title = t("reminderStatusUnavailable");

    description = t("reminderStatusUnavailableDescription");
  } else if (connectedChannels.length === 0 && pendingChannels.length > 0) {
    summaryIcon = <RefreshCw className="size-6 text-[#c93400]" />;

    summaryIconClassName = "bg-[#ff9500]/10";

    badge = t("connectionPending");

    badgeClassName = "bg-[#ff9500]/10 text-[#c93400]";

    title = t("channelsPendingTitle");

    description = t("channelsPendingDescription");
  } else if (connectedChannels.length === 0) {
    summaryIcon = <BellOff className="size-6 text-[#d7003a]" />;

    summaryIconClassName = "bg-[#ff2d55]/10";

    badge = t("remindersOffStatus");

    badgeClassName = "bg-[#ff2d55]/10 text-[#d7003a]";

    title = t("remindersOff");

    description = t("remindersOffDescription");
  } else if (failedWithRetry?.latestNotification?.nextRetryAt) {
    summaryIcon = <RefreshCw className="size-6 text-[#c93400]" />;

    summaryIconClassName = "bg-[#ff9500]/10";

    badge = t("retryScheduled");

    badgeClassName = "bg-[#ff9500]/10 text-[#c93400]";

    title = t("deliveryFailed");

    description = t("channelRetryingAt", {
      channel: failedWithRetry.label,

      time: formatInstant(failedWithRetry.latestNotification.nextRetryAt, locale, timezone),
    });
  } else if (failedWithoutRetry) {
    summaryIcon = <CircleAlert className="size-6 text-[#d70015]" />;

    summaryIconClassName = "bg-[#ff3b30]/10";

    badge = t("actionNeeded");

    badgeClassName = "bg-[#ff3b30]/10 text-[#d70015]";

    title = t("deliveryStopped");

    description = t("channelDeliveryStopped", {
      channel: failedWithoutRetry.label,
    });
  } else {
    summaryIcon = <BellRing className="size-6 text-[#007aff]" />;

    summaryIconClassName = "bg-[#007aff]/10";

    badge = t("active");

    badgeClassName = "bg-[#34c759]/10 text-[#248a3d]";

    title = t("dailyRemindersActive");

    description =
      connectedChannels.length === 2
        ? t("dailyRemindersBothChannels")
        : t("dailyRemindersOneChannel", {
            channel: connectedChannels[0].label,
          });
  }

  return (
    <section className="mt-5">
      <h2 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">
        {t("reminderStatus")}
      </h2>

      <article className="overflow-hidden rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
        <div className="flex items-start gap-4 p-5">
          <div className={`flex size-12 shrink-0 items-center justify-center rounded-[15px] ${summaryIconClassName}`}>
            {summaryIcon}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-[17px] font-semibold text-[#1c1c1e]">{title}</h3>

              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${badgeClassName}`}>{badge}</span>
            </div>

            <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{description}</p>

            {connectedChannels.length > 0 && (
              <p className="mt-2 text-xs font-medium text-[#248a3d]">
                {t("connectedChannels", {
                  count: connectedChannels.length,
                })}
              </p>
            )}
          </div>
        </div>

        {connectedChannels.length > 0 && nextSchedule && (
          <div className="mx-5 mb-5 rounded-[16px] bg-[#f2f2f7] px-4 py-3">
            <div className="flex items-start gap-3">
              <div className="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-white">
                <Clock3 className="size-4 text-[#007aff]" />
              </div>

              <div className="min-w-0">
                <p className="text-xs font-medium text-[#8e8e93]">{t("dailyReminderSchedule")}</p>

                <p className="mt-0.5 text-sm font-semibold leading-5 text-[#1c1c1e]">{nextSchedule}</p>

                <p className="mt-1 text-xs leading-5 text-[#8e8e93]">{t("dailyReminderContinues")}</p>
              </div>
            </div>
          </div>
        )}

        <div className="border-t border-black/6">
          {channels.map((channel, index) => (
            <div key={channel.channelType}>
              {index > 0 && <div className="ml-16 border-t border-black/6" />}

              <ReminderChannelRow channel={channel} locale={locale} timezone={timezone} />
            </div>
          ))}
        </div>

        <div className="grid border-t border-black/6 sm:grid-cols-2 sm:divide-x sm:divide-black/6">
          <Link
            href="/settings"
            className="flex min-h-13 items-center justify-between px-5 text-sm font-semibold text-[#007aff] transition hover:bg-black/3"
          >
            <span>{t("manageReminderSettings")}</span>

            <ChevronRight className="size-4" />
          </Link>

          <Link
            href="/notifications"
            className="flex min-h-13 items-center justify-between border-t border-black/6 px-5 text-sm font-semibold text-[#5856d6] transition hover:bg-black/3 sm:border-t-0"
          >
            <span>{t("viewNotificationHistory")}</span>

            <ChevronRight className="size-4" />
          </Link>
        </div>
      </article>
    </section>
  );
}

type ReminderChannelRowProps = {
  channel: ReminderChannelView;

  locale: string;
  timezone: string;
};

function ReminderChannelRow({ channel, locale, timezone }: ReminderChannelRowProps) {
  const t = useTranslations("Dashboard");

  const isTelegram = channel.channelType === "TELEGRAM";

  const notification = channel.latestNotification;

  let badge: string;

  let badgeClassName: string;

  let description: string;

  if (channel.status === "UNAVAILABLE") {
    badge = t("channelStatusUnavailable");

    badgeClassName = "bg-[#8e8e93]/10 text-[#636366]";

    description = t("channelStatusUnavailableDescription");
  } else if (channel.status === "PENDING") {
    badge = t("channelPending");

    badgeClassName = "bg-[#ff9500]/10 text-[#c93400]";

    description = t("channelPendingDescription");
  } else if (channel.status === "ALREADY_LINKED") {
    badge = t("channelAlreadyLinked");

    badgeClassName = "bg-[#ff3b30]/10 text-[#d70015]";

    description = t("channelAlreadyLinkedDescription");
  } else if (!channel.connected) {
    badge = t("channelDisconnected");

    badgeClassName = "bg-[#8e8e93]/10 text-[#636366]";

    description = t("channelDisconnectedDescription");
  } else if (notification?.status === "FAILED" && notification.nextRetryAt) {
    badge = t("channelRetry");

    badgeClassName = "bg-[#ff9500]/10 text-[#c93400]";

    description = t("channelLatestRetryAt", {
      time: formatInstant(notification.nextRetryAt, locale, timezone),
    });
  } else if (notification?.status === "FAILED") {
    badge = t("channelFailed");

    badgeClassName = "bg-[#ff3b30]/10 text-[#d70015]";

    description = t("channelLatestFailed");
  } else if (notification?.status === "SENT" && notification.sentAt) {
    badge = t("channelSent");

    badgeClassName = "bg-[#34c759]/10 text-[#248a3d]";

    description = t("channelLatestSentAt", {
      time: formatInstant(notification.sentAt, locale, timezone),
    });
  } else {
    badge = t("channelReady");

    badgeClassName = "bg-[#007aff]/10 text-[#0062cc]";

    description = channel.username
      ? isTelegram
        ? t("telegramConnectedAs", {
            username: channel.username,
          })
        : t("discordConnectedAs", {
            username: channel.username,
          })
      : t("channelConnected");
  }

  return (
    <div className="flex items-start gap-4 px-5 py-4">
      <div
        className={
          isTelegram
            ? "flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#229ed9]/10"
            : "flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#5865f2]/10"
        }
      >
        {isTelegram ? <Send className="size-5 text-[#229ed9]" /> : <Gamepad2 className="size-5 text-[#5865f2]" />}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h4 className="text-[15px] font-semibold text-[#1c1c1e]">{channel.label}</h4>

          <span className={`rounded-full px-2 py-1 text-[10px] font-semibold ${badgeClassName}`}>{badge}</span>
        </div>

        <p className="mt-1 text-xs leading-5 text-[#8e8e93]">{description}</p>
      </div>
    </div>
  );
}

type InfoRowProps = {
  icon: ReactNode;
  label: string;
  value: string;
};

function InfoRow({ icon, label, value }: InfoRowProps) {
  return (
    <div className="flex min-h-15.5 items-center gap-3 px-4 py-3">
      <div className="flex size-9 shrink-0 items-center justify-center rounded-[11px] bg-[#f2f2f7]">{icon}</div>

      <p className="flex-1 text-[15px] text-[#3a3a3c]">{label}</p>

      <p className="max-w-[45%] truncate text-right text-[15px] text-[#8e8e93]">{value}</p>
    </div>
  );
}
