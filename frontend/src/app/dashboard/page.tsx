"use client";

import { type ReactNode, useCallback, useEffect, useState } from "react";

import {
  Bell,
  BellOff,
  BellRing,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
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

import { getNotificationHistory } from "@/lib/api/notification-history-api";
import { getSettings } from "@/lib/api/settings-api";
import { getTelegramConnection } from "@/lib/api/telegram-api";
import { getCurrentUser } from "@/lib/api/user-api";

import { useAuthStore } from "@/stores/auth-store";

import type { AuthUser } from "@/types/auth";

import type { CyclePrediction, CycleRecord } from "@/types/cycle";

import type { NotificationHistoryItem } from "@/types/notification-history";
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

type DashboardData = {
  currentUser: AuthUser;
  history: CycleRecord[];
  prediction: CyclePrediction | null;
  settings: Settings | null;

  telegramConnection: TelegramConnection | null;

  latestNotification: NotificationHistoryItem | null;
};

async function fetchDashboardData(accessToken: string): Promise<DashboardData> {
  const [currentUser, history] = await Promise.all([getCurrentUser(accessToken), getCycleHistory(accessToken)]);

  if (history.length === 0) {
    return {
      currentUser,
      history,
      prediction: null,
      settings: null,
      telegramConnection: null,
      latestNotification: null,
    };
  }

  const [predictionResult, settingsResult, telegramResult, notificationHistoryResult] = await Promise.allSettled([
    getCyclePrediction(accessToken),

    getSettings(accessToken),

    getTelegramConnection(accessToken),

    getNotificationHistory(accessToken, {
      page: 0,
      size: 1,
    }),
  ]);

  const prediction = getSettledValue(predictionResult);

  const settings = getSettledValue(settingsResult);

  const telegramConnection = getSettledValue(telegramResult);

  const notificationHistory = getSettledValue(notificationHistoryResult);

  return {
    currentUser,
    history,
    prediction,
    settings,
    telegramConnection,

    latestNotification: notificationHistory?.items[0] ?? null,
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

  const [latestNotification, setLatestNotification] = useState<NotificationHistoryItem | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const applyDashboardData = useCallback(
    (data: DashboardData) => {
      setUser(data.currentUser);

      setHistory(data.history);

      setPrediction(data.prediction);

      setSettings(data.settings);

      setTelegramConnection(data.telegramConnection);

      setLatestNotification(data.latestNotification);

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
        latestNotification={latestNotification}
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

  latestNotification: NotificationHistoryItem | null;

  locale: string;
};

function ReminderStatusCard({
  prediction,
  settings,
  telegramConnection,
  latestNotification,
  locale,
}: ReminderStatusCardProps) {
  const t = useTranslations("Dashboard");

  const timezone = settings?.timezone ?? "UTC";

  const nextSchedule =
    prediction && settings
      ? [
          formatDate(prediction.reminderDate, locale, "long"),

          formatTime(settings.notificationTime, locale),

          settings.timezone,
        ].join(" · ")
      : null;

  if (!telegramConnection) {
    return (
      <ReminderCardFrame
        icon={<Bell className="size-6 text-[#636366]" />}
        iconClassName="bg-[#8e8e93]/10"
        badge={t("statusUnavailable")}
        badgeClassName="bg-[#8e8e93]/10 text-[#636366]"
        title={t("reminderStatusUnavailable")}
        description={t("reminderStatusUnavailableDescription")}
        actionHref="/settings"
        actionLabel={t("manageReminders")}
      />
    );
  }

  if (telegramConnection.status === "PENDING") {
    return (
      <ReminderCardFrame
        icon={<RefreshCw className="size-6 text-[#c93400]" />}
        iconClassName="bg-[#ff9500]/10"
        badge={t("connectionPending")}
        badgeClassName="bg-[#ff9500]/10 text-[#c93400]"
        title={t("telegramPending")}
        description={t("telegramPendingDescription")}
        actionHref="/settings"
        actionLabel={t("finishTelegramConnection")}
      />
    );
  }

  if (!telegramConnection.connected) {
    return (
      <ReminderCardFrame
        icon={<BellOff className="size-6 text-[#d7003a]" />}
        iconClassName="bg-[#ff2d55]/10"
        badge={t("remindersOffStatus")}
        badgeClassName="bg-[#ff2d55]/10 text-[#d7003a]"
        title={t("remindersOff")}
        description={t("remindersOffDescription")}
        actionHref="/settings"
        actionLabel={t("connectTelegram")}
      />
    );
  }

  if (latestNotification?.status === "FAILED" && latestNotification.nextRetryAt) {
    return (
      <ReminderCardFrame
        icon={<RefreshCw className="size-6 text-[#c93400]" />}
        iconClassName="bg-[#ff9500]/10"
        badge={t("retryScheduled")}
        badgeClassName="bg-[#ff9500]/10 text-[#c93400]"
        title={t("deliveryFailed")}
        description={t("retryingAt", {
          time: formatInstant(latestNotification.nextRetryAt, locale, timezone),
        })}
        detailLabel={t("deliveryAttempts")}
        detailValue={t("attemptCount", {
          count: latestNotification.attemptCount,
        })}
        actionHref="/notifications"
        actionLabel={t("viewDetails")}
      />
    );
  }

  if (latestNotification?.status === "FAILED") {
    return (
      <ReminderCardFrame
        icon={<CircleAlert className="size-6 text-[#d70015]" />}
        iconClassName="bg-[#ff3b30]/10"
        badge={t("actionNeeded")}
        badgeClassName="bg-[#ff3b30]/10 text-[#d70015]"
        title={t("deliveryStopped")}
        description={t("failedAfterAttempts", {
          count: latestNotification.attemptCount,
        })}
        actionHref="/notifications"
        actionLabel={t("viewDetails")}
      />
    );
  }

  if (latestNotification?.status === "SENT" && latestNotification.sentAt) {
    return (
      <ReminderCardFrame
        icon={<CheckCircle2 className="size-6 text-[#248a3d]" />}
        iconClassName="bg-[#34c759]/10"
        badge={t("sent")}
        badgeClassName="bg-[#34c759]/10 text-[#248a3d]"
        title={t("latestReminder")}
        description={t("latestReminderSentAt", {
          time: formatInstant(latestNotification.sentAt, locale, timezone),
        })}
        detailLabel={telegramConnection.telegramUsername ? t("sentTo") : undefined}
        detailValue={telegramConnection.telegramUsername ? `@${telegramConnection.telegramUsername}` : undefined}
        actionHref="/notifications"
        actionLabel={t("viewNotificationHistory")}
      />
    );
  }

  const connectedDescription = telegramConnection.telegramUsername
    ? t("connectedAs", {
        username: telegramConnection.telegramUsername,
      })
    : t("telegramConnected");

  return (
    <ReminderCardFrame
      icon={<BellRing className="size-6 text-[#007aff]" />}
      iconClassName="bg-[#007aff]/10"
      badge={t("telegramConnected")}
      badgeClassName="bg-[#007aff]/10 text-[#0062cc]"
      title={t("nextReminder")}
      description={nextSchedule ? t("nextReminderDescription") : t("scheduleUnavailable")}
      detailLabel={nextSchedule ? t("nextReminderSchedule") : undefined}
      detailValue={nextSchedule ?? undefined}
      secondaryText={connectedDescription}
      actionHref="/settings"
      actionLabel={t("manageReminderSettings")}
    />
  );
}

type ReminderCardFrameProps = {
  icon: ReactNode;
  iconClassName: string;
  badge: string;
  badgeClassName: string;
  title: string;
  description: string;
  detailLabel?: string;
  detailValue?: string;
  secondaryText?: string;
  actionHref: string;
  actionLabel: string;
};

function ReminderCardFrame({
  icon,
  iconClassName,
  badge,
  badgeClassName,
  title,
  description,
  detailLabel,
  detailValue,
  secondaryText,
  actionHref,
  actionLabel,
}: ReminderCardFrameProps) {
  const t = useTranslations("Dashboard");

  return (
    <section className="mt-5">
      <h2 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">
        {t("reminderStatus")}
      </h2>

      <article className="overflow-hidden rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
        <div className="flex items-start gap-4 p-5">
          <div className={`flex size-12 shrink-0 items-center justify-center rounded-[15px] ${iconClassName}`}>
            {icon}
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="text-[17px] font-semibold text-[#1c1c1e]">{title}</h3>

              <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${badgeClassName}`}>{badge}</span>
            </div>

            <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{description}</p>

            {secondaryText && (
              <div className="mt-2 flex items-center gap-1.5 text-xs font-medium text-[#229ed9]">
                <Send className="size-3.5" />

                <span>{secondaryText}</span>
              </div>
            )}
          </div>
        </div>

        {detailLabel && detailValue && (
          <div className="mx-5 mb-5 flex items-start gap-3 rounded-[16px] bg-[#f2f2f7] px-4 py-3">
            <div className="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-white">
              <Clock3 className="size-4 text-[#007aff]" />
            </div>

            <div className="min-w-0">
              <p className="text-xs font-medium text-[#8e8e93]">{detailLabel}</p>

              <p className="mt-0.5 text-sm font-semibold leading-5 text-[#1c1c1e]">{detailValue}</p>
            </div>
          </div>
        )}

        <Link
          href={actionHref}
          className="flex min-h-13 items-center justify-between border-t border-black/6 px-5 text-sm font-semibold text-[#007aff] transition hover:bg-black/3"
        >
          <span>{actionLabel}</span>

          <ChevronRight className="size-4" />
        </Link>
      </article>
    </section>
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
