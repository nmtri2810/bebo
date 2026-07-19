"use client";

import { type FormEvent, useEffect, useState } from "react";

import { CalendarDays, HeartPulse, ShieldCheck } from "lucide-react";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { createCycleRecord, getCycleHistory } from "@/lib/api/cycle-api";

import { ApiClientError } from "@/lib/api/api-client";

import { useAuthStore } from "@/stores/auth-store";

function getTodayInputValue(): string {
  const today = new Date();

  const year = today.getFullYear();

  const month = String(today.getMonth() + 1).padStart(2, "0");

  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiClientError) {
    const firstFieldError = Object.values(error.fieldErrors)[0];

    return firstFieldError ?? error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

export default function OnboardingPage() {
  const router = useRouter();
  const t = useTranslations("Onboarding");

  const accessToken = useAuthStore((state) => state.accessToken);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [startDate, setStartDate] = useState("");

  const [isChecking, setIsChecking] = useState(true);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const today = getTodayInputValue();

  /*
   * Route guard:
   *
   * - Chưa login → /
   * - Đã có cycle record → /dashboard
   * - Chưa có record → hiển thị onboarding
   */
  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    const checkOnboardingState = async () => {
      try {
        const history = await getCycleHistory(accessToken);

        if (cancelled) {
          return;
        }

        if (history.length > 0) {
          router.replace("/dashboard");
          return;
        }

        setIsChecking(false);
      } catch (error) {
        if (cancelled) {
          return;
        }

        if (error instanceof ApiClientError && error.status === 401) {
          clearSession();
          router.replace("/");
          return;
        }

        setErrorMessage(getErrorMessage(error, t("genericError")));

        setIsChecking(false);
      }
    };

    void checkOnboardingState();

    return () => {
      cancelled = true;
    };
  }, [accessToken, clearSession, hasHydrated, router, t]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken || !startDate) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await createCycleRecord(accessToken, {
        startDate,
      });

      router.replace("/dashboard");
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        clearSession();
        router.replace("/");
        return;
      }

      setErrorMessage(getErrorMessage(error, t("genericError")));
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!hasHydrated || isChecking) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <div className="flex flex-col items-center gap-3">
          <div className="size-6 animate-spin rounded-full border-2 border-[#007aff]/20 border-t-[#007aff]" />

          <p className="text-sm text-[#8e8e93]">Loading...</p>
        </div>
      </main>
    );
  }

  if (!accessToken) {
    return null;
  }

  return (
    <main className="flex min-h-dvh items-center justify-center bg-[#f2f2f7] px-4 py-14">
      <div className="relative w-full max-w-110">
        <div className="absolute -top-12 right-0">
          <LanguageSwitcher />
        </div>

        <Card className="overflow-hidden rounded-[30px] border border-black/6 bg-white shadow-[0_12px_40px_rgba(0,0,0,0.08)]">
          <CardHeader className="flex flex-col items-center px-6 pb-4 pt-8 text-center">
            <div className="mb-4 flex size-17 items-center justify-center rounded-[20px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_8px_20px_rgba(255,45,85,0.25)]">
              <HeartPulse className="size-9 text-white" strokeWidth={2.2} />
            </div>

            <p className="mb-2 text-[13px] font-semibold uppercase tracking-[0.08em] text-[#ff2d55]">{t("step")}</p>

            <CardTitle className="max-w-87.5 text-[28px] font-bold leading-[1.12] tracking-[-0.04em] text-[#1c1c1e]">
              {t("title")}
            </CardTitle>

            <p className="mt-3 max-w-87.5 text-[15px] leading-6 text-[#636366]">{t("description")}</p>
          </CardHeader>

          <CardContent className="px-6 pb-7">
            <form className="mt-3 space-y-5" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="startDate" className="text-[13px] font-semibold text-[#3a3a3c]">
                  {t("dateLabel")}
                </Label>

                <div className="relative">
                  <CalendarDays className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-[#007aff]" />

                  <Input
                    id="startDate"
                    name="startDate"
                    type="date"
                    max={today}
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                    className="h-14 rounded-[14px] border-0 bg-[#f2f2f7] pl-12 pr-4 text-[16px] font-medium text-[#1c1c1e] shadow-none focus-visible:ring-2 focus-visible:ring-[#007aff]/30"
                    required
                  />
                </div>

                <p className="px-1 text-xs text-[#8e8e93]">{t("dateHint")}</p>
              </div>

              {errorMessage && (
                <div role="alert" className="rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
                  {errorMessage}
                </div>
              )}

              <Button
                type="submit"
                disabled={isSubmitting || !startDate}
                className="h-12 w-full rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6] active:scale-[0.99]"
              >
                {isSubmitting ? t("saving") : t("continue")}
              </Button>

              <div className="flex items-start justify-center gap-2 px-3 text-center text-xs leading-5 text-[#8e8e93]">
                <ShieldCheck className="mt-0.5 size-4 shrink-0" />

                <span>{t("privacy")}</span>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
