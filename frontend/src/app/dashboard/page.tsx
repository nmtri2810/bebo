"use client";

import { useEffect, useState } from "react";

import { Check, HeartPulse } from "lucide-react";

import { useLocale, useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { getCycleHistory } from "@/lib/api/cycle-api";

import { ApiClientError } from "@/lib/api/api-client";

import { useAuthStore } from "@/stores/auth-store";

import type { CycleRecord } from "@/types/cycle";

function formatDate(value: string, locale: string): string {
  const [year, month, day] = value.split("-").map(Number);

  const date = new Date(year, month - 1, day);

  return new Intl.DateTimeFormat(locale, {
    dateStyle: "long",
  }).format(date);
}

export default function DashboardPage() {
  const router = useRouter();
  const locale = useLocale();
  const t = useTranslations("Dashboard");

  const accessToken = useAuthStore((state) => state.accessToken);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [history, setHistory] = useState<CycleRecord[]>([]);

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

    const loadHistory = async () => {
      try {
        const result = await getCycleHistory(accessToken);

        if (cancelled) {
          return;
        }

        if (result.length === 0) {
          router.replace("/onboarding");
          return;
        }

        setHistory(result);
      } catch (error) {
        if (cancelled) {
          return;
        }

        if (error instanceof ApiClientError && error.status === 401) {
          clearSession();
          router.replace("/");
          return;
        }

        setErrorMessage(t("genericError"));
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void loadHistory();

    return () => {
      cancelled = true;
    };
  }, [accessToken, clearSession, hasHydrated, router, t]);

  const handleLogout = () => {
    clearSession();
    router.replace("/");
  };

  if (!hasHydrated || isLoading) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <p className="text-sm text-[#8e8e93]">{t("loading")}</p>
      </main>
    );
  }

  if (!accessToken) {
    return null;
  }

  const latestRecord = history[0];

  return (
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-8">
      <div className="mx-auto w-full max-w-110">
        <header className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="flex size-10 items-center justify-center rounded-[13px] bg-[#ff2d55]">
              <HeartPulse className="size-5 text-white" />
            </div>

            <span className="text-xl font-bold tracking-[-0.04em]">bebo</span>
          </div>

          <LanguageSwitcher />
        </header>

        <Card className="rounded-[28px] border-0 bg-white shadow-[0_8px_30px_rgba(0,0,0,0.06)]">
          <CardHeader className="flex flex-col items-center px-6 pb-5 pt-8 text-center">
            <div className="mb-4 flex size-14 items-center justify-center rounded-full bg-[#34c759]/15">
              <Check className="size-7 text-[#248a3d]" strokeWidth={3} />
            </div>

            <CardTitle className="text-[26px] font-bold tracking-[-0.04em]">{t("title")}</CardTitle>

            <p className="mt-1 text-[15px] text-[#636366]">{t("description")}</p>
          </CardHeader>

          <CardContent className="space-y-5 px-6 pb-7">
            {errorMessage && (
              <div className="rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">{errorMessage}</div>
            )}

            {latestRecord && (
              <section>
                <p className="mb-2 px-1 text-xs font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">
                  {t("latestPeriod")}
                </p>

                <div className="rounded-2xl bg-[#f2f2f7] px-4 py-4">
                  <p className="text-[17px] font-semibold text-[#1c1c1e]">
                    {formatDate(latestRecord.startDate, locale)}
                  </p>
                </div>
              </section>
            )}

            <p className="text-center text-sm leading-6 text-[#8e8e93]">{t("dashboardComing")}</p>

            <Button
              type="button"
              variant="ghost"
              onClick={handleLogout}
              className="h-12 w-full rounded-[14px] bg-[#ff3b30]/10 text-[16px] font-semibold text-[#d70015] hover:bg-[#ff3b30]/15 hover:text-[#d70015]"
            >
              {t("logout")}
            </Button>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
