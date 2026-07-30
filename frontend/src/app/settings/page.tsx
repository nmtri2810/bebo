"use client";

import { type FormEvent, useEffect, useState } from "react";

import { Bell, BellRing, CalendarDays, Check, ChevronRight, Clock3, Globe2, SlidersHorizontal } from "lucide-react";

import Link from "next/link";

import { useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { AppShell } from "@/components/app-shell";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { TelegramConnectionCard } from "@/features/telegram/components/telegram-connection-card";

import { ApiClientError } from "@/lib/api/api-client";

import { getSettings, updateSettings } from "@/lib/api/settings-api";

import { useAuthStore } from "@/stores/auth-store";

import type { Settings } from "@/types/settings";

type SettingsForm = {
  defaultCycleLength: string;
  reminderDaysBefore: string;
  notificationTime: string;
  timezone: string;
};

function toForm(settings: Settings): SettingsForm {
  return {
    defaultCycleLength: String(settings.defaultCycleLength),

    reminderDaysBefore: String(settings.reminderDaysBefore),

    notificationTime: settings.notificationTime,

    timezone: settings.timezone,
  };
}

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiClientError) {
    const fieldError = Object.values(error.fieldErrors)[0];

    return fieldError ?? error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

export default function SettingsPage() {
  const router = useRouter();
  const t = useTranslations("Settings");

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const setUser = useAuthStore((state) => state.setUser);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [form, setForm] = useState<SettingsForm | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [isSaving, setIsSaving] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [savedMessage, setSavedMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    getSettings(accessToken)
      .then((settings) => {
        if (!cancelled) {
          setForm(toForm(settings));
        }
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

        setErrorMessage(getErrorMessage(error, t("loadError")));
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

  const updateField = (field: keyof SettingsForm, value: string) => {
    setForm((current) => {
      if (!current) {
        return current;
      }

      return {
        ...current,
        [field]: value,
      };
    });

    setSavedMessage(null);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken || !form) {
      return;
    }

    setErrorMessage(null);
    setSavedMessage(null);
    setIsSaving(true);

    try {
      const savedSettings = await updateSettings(accessToken, {
        defaultCycleLength: Number(form.defaultCycleLength),

        reminderDaysBefore: Number(form.reminderDaysBefore),

        notificationTime: form.notificationTime,

        timezone: form.timezone.trim(),
      });

      setForm(toForm(savedSettings));

      if (user) {
        setUser({
          ...user,
          timezone: savedSettings.timezone,
        });
      }

      setSavedMessage(t("saved"));
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        clearSession();
        router.replace("/");

        return;
      }

      setErrorMessage(getErrorMessage(error, t("saveError")));
    } finally {
      setIsSaving(false);
    }
  };

  if (!hasHydrated || isLoading) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <p className="text-sm text-[#8e8e93]">{t("loading")}</p>
      </main>
    );
  }

  if (!accessToken || !form) {
    return null;
  }

  return (
    <AppShell maxWidthClassName="max-w-6xl">
      <div className="mb-8 flex items-center gap-3 lg:mb-10 lg:block">
        <div className="flex size-12 shrink-0 items-center justify-center rounded-[15px] bg-linear-to-br from-[#5856d6] to-[#007aff] shadow-[0_6px_16px_rgba(88,86,214,0.2)] lg:hidden">
          <SlidersHorizontal className="size-6 text-white" />
        </div>

        <div>
          <h1 className="text-[30px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e] lg:text-[40px]">
            {t("title")}
          </h1>

          <p className="mt-1 max-w-2xl text-sm leading-5 text-[#8e8e93] lg:mt-2 lg:text-[16px] lg:leading-6">
            {t("description")}
          </p>
        </div>
      </div>

      <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1.45fr)_minmax(320px,0.75fr)]">
        <form className="min-w-0 space-y-7" onSubmit={handleSubmit}>
          <SettingsSection icon={<CalendarDays className="size-5 text-[#ff2d55]" />} title={t("cycleSection")}>
            <SettingsField label={t("defaultCycleLength")} hint={t("defaultCycleLengthHint")}>
              <div className="relative">
                <Input
                  type="number"
                  min={15}
                  max={60}
                  value={form.defaultCycleLength}
                  onChange={(event) => updateField("defaultCycleLength", event.target.value)}
                  className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] pr-16 text-[16px] shadow-none"
                  required
                />

                <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-[#8e8e93]">
                  {t("days")}
                </span>
              </div>
            </SettingsField>
          </SettingsSection>

          <SettingsSection icon={<Bell className="size-5 text-[#007aff]" />} title={t("reminderSection")}>
            <div className="grid gap-5 sm:grid-cols-2">
              <SettingsField label={t("reminderDaysBefore")} hint={t("reminderDaysHint")}>
                <div className="relative">
                  <Input
                    type="number"
                    min={0}
                    max={14}
                    value={form.reminderDaysBefore}
                    onChange={(event) => updateField("reminderDaysBefore", event.target.value)}
                    className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] pr-16 text-[16px] shadow-none"
                    required
                  />

                  <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-[#8e8e93]">
                    {t("days")}
                  </span>
                </div>
              </SettingsField>

              <SettingsField label={t("notificationTime")}>
                <div className="relative">
                  <Clock3 className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-[#007aff]" />

                  <Input
                    type="time"
                    value={form.notificationTime}
                    onChange={(event) => updateField("notificationTime", event.target.value)}
                    className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] pl-12 text-[16px] shadow-none"
                    required
                  />
                </div>
              </SettingsField>
            </div>
          </SettingsSection>

          <SettingsSection icon={<Globe2 className="size-5 text-[#34c759]" />} title={t("accountSection")}>
            <SettingsField label={t("timezone")} hint={t("timezoneHint")}>
              <Input
                type="text"
                value={form.timezone}
                onChange={(event) => updateField("timezone", event.target.value)}
                placeholder="Asia/Ho_Chi_Minh"
                className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] text-[16px] shadow-none"
                required
              />
            </SettingsField>
          </SettingsSection>

          {errorMessage && (
            <div role="alert" className="rounded-[16px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
              {errorMessage}
            </div>
          )}

          {savedMessage && (
            <div className="flex items-center gap-2 rounded-[16px] bg-[#34c759]/10 px-4 py-3 text-sm font-medium text-[#248a3d]">
              <Check className="size-4" />

              {savedMessage}
            </div>
          )}

          <Button
            type="submit"
            disabled={isSaving}
            className="h-12 w-full rounded-[15px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
          >
            {isSaving ? t("saving") : t("save")}
          </Button>

          <p className="text-center text-xs text-[#8e8e93]">{t("changesHint")}</p>
        </form>

        <aside className="min-w-0 space-y-8">
          <TelegramConnectionCard accessToken={accessToken} />

          <section>
            <SettingsSectionTitle title={t("activitySection")} />

            <Link
              href="/notifications"
              className="flex min-h-19 items-center gap-4 rounded-[22px] bg-white px-5 py-4 shadow-[0_5px_20px_rgba(0,0,0,0.05)] transition hover:bg-[#fafafa] active:scale-[0.995]"
            >
              <div className="flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#5856d6]/10">
                <BellRing className="size-5 text-[#5856d6]" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-[16px] font-semibold text-[#1c1c1e]">{t("notificationHistory")}</p>

                <p className="mt-0.5 text-[13px] leading-5 text-[#8e8e93]">{t("notificationHistoryHint")}</p>
              </div>

              <ChevronRight className="size-5 shrink-0 text-[#c7c7cc]" />
            </Link>
          </section>
        </aside>
      </div>
    </AppShell>
  );
}

type SettingsSectionProps = {
  icon: React.ReactNode;
  title: string;
  children: React.ReactNode;
};

function SettingsSection({ icon, title, children }: SettingsSectionProps) {
  return (
    <section>
      <div className="mb-3 flex items-center gap-2 px-1">
        {icon}

        <h2 className="text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">{title}</h2>
      </div>

      <div className="space-y-5 rounded-[24px] bg-white p-5 shadow-[0_6px_24px_rgba(0,0,0,0.055)] sm:p-6">
        {children}
      </div>
    </section>
  );
}

type SettingsSectionTitleProps = {
  title: string;
};

function SettingsSectionTitle({ title }: SettingsSectionTitleProps) {
  return <h2 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">{title}</h2>;
}

type SettingsFieldProps = {
  label: string;
  hint?: string;
  children: React.ReactNode;
};

function SettingsField({ label, hint, children }: SettingsFieldProps) {
  return (
    <div className="space-y-2">
      <Label className="text-[14px] font-semibold text-[#3a3a3c]">{label}</Label>

      {children}

      {hint && <p className="px-1 text-xs leading-5 text-[#8e8e93]">{hint}</p>}
    </div>
  );
}
