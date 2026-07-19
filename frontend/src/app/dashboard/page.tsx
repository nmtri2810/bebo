"use client";

import { useCallback, useEffect, useState } from "react";

import { Bell, CalendarDays, HeartPulse, LogOut, Settings2 } from "lucide-react";

import { useLocale, useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";

import { AddCycleDialog } from "@/features/cycles/components/add-cycle-dialog";

import { ApiClientError } from "@/lib/api/api-client";

import { getCycleHistory, getCyclePrediction } from "@/lib/api/cycle-api";

import { useAuthStore } from "@/stores/auth-store";

import type { CyclePrediction, CycleRecord } from "@/types/cycle";
import { ManageCycleDialog } from "@/features/cycles/components/manage-cycle-dialog";
import Link from "next/link";

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

type DashboardData = {
  history: CycleRecord[];
  prediction: CyclePrediction | null;
};

async function fetchDashboardData(accessToken: string): Promise<DashboardData> {
  const history = await getCycleHistory(accessToken);

  if (history.length === 0) {
    return {
      history,
      prediction: null,
    };
  }

  try {
    const prediction = await getCyclePrediction(accessToken);

    return {
      history,
      prediction,
    };
  } catch (error) {
    /*
     * Lỗi 401 cần được truyền ra ngoài
     * để component xóa session.
     */
    if (error instanceof ApiClientError && error.status === 401) {
      throw error;
    }

    /*
     * History vẫn dùng được nếu riêng
     * prediction đang tạm thời lỗi.
     */
    return {
      history,
      prediction: null,
    };
  }
}

export default function DashboardPage() {
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations("Dashboard");

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [history, setHistory] = useState<CycleRecord[]>([]);

  const [prediction, setPrediction] = useState<CyclePrediction | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

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

        if (data.history.length === 0) {
          router.replace("/onboarding");
          return;
        }

        setHistory(data.history);
        setPrediction(data.prediction);
        setErrorMessage(null);
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
  }, [accessToken, clearSession, hasHydrated, router, t]);

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

      if (data.history.length === 0) {
        router.replace("/onboarding");
        return;
      }

      setHistory(data.history);
      setPrediction(data.prediction);
      setErrorMessage(null);
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        clearSession();
        router.replace("/");
        return;
      }

      setErrorMessage(t("loadError"));

      throw error;
    }
  }, [accessToken, clearSession, router, t]);

  const handleLogout = () => {
    clearSession();
    router.replace("/");
  };

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
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-6 sm:py-8">
      <div className="mx-auto w-full max-w-130">
        <header className="mb-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-[14px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_6px_16px_rgba(255,45,85,0.22)]">
              <HeartPulse className="size-6 text-white" />
            </div>

            <div>
              <h1 className="text-xl font-bold leading-5 tracking-[-0.04em] text-[#1c1c1e]">bebo</h1>

              <p className="mt-1 text-xs text-[#8e8e93]">{t("brandSubtitle")}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href="/settings"
              aria-label={t("settings")}
              title={t("settings")}
              className="inline-flex size-9 items-center justify-center rounded-full border border-black/6 bg-white text-[#636366] shadow-[0_3px_10px_rgba(0,0,0,0.05)] transition hover:bg-[#f9f9fb] hover:text-[#007aff]"
            >
              <Settings2 className="size-4" />
            </Link>

            <LanguageSwitcher />
          </div>
        </header>

        <div className="mb-5 flex items-end justify-between gap-4">
          <div>
            <h2 className="text-[30px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e]">{t("greeting")}</h2>

            {user?.email && <p className="mt-1 max-w-75 truncate text-sm text-[#8e8e93]">{user.email}</p>}
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
          <h3 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">
            {t("recentHistory")}
          </h3>

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

        <Button
          type="button"
          variant="ghost"
          onClick={handleLogout}
          className="mt-8 h-12 w-full rounded-[15px] text-[15px] font-semibold text-[#d70015] hover:bg-[#ff3b30]/10 hover:text-[#d70015]"
        >
          <LogOut className="size-4" />
          {t("logout")}
        </Button>
      </div>
    </main>
  );
}

type InfoRowProps = {
  icon: React.ReactNode;
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
