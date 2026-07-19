"use client";

import { type FormEvent, useEffect, useState } from "react";

import { Bell, CalendarDays, Check, ChevronLeft, Clock3, Globe2, HeartPulse } from "lucide-react";

import Link from "next/link";

import { useLocale, useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

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
  const locale = useLocale();
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
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-6 sm:py-8">
      <div className="mx-auto w-full max-w-130">
        <header className="mb-8 flex items-center justify-between">
          <Link
            href="/dashboard"
            className="inline-flex h-10 items-center gap-1 rounded-full px-2 text-sm font-semibold text-[#007aff] transition hover:bg-black/4"
          >
            <ChevronLeft className="size-5" />

            {t("back")}
          </Link>

          <LanguageSwitcher />
        </header>

        <div className="mb-7 flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-[15px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_6px_16px_rgba(255,45,85,0.2)]">
            <HeartPulse className="size-6 text-white" />
          </div>

          <div>
            <h1 className="text-[30px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e]">{t("title")}</h1>

            <p className="mt-1 text-sm text-[#8e8e93]">{t("description")}</p>
          </div>
        </div>

        <form className="space-y-7" onSubmit={handleSubmit}>
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

          <p className="text-center text-xs text-[#8e8e93]">
            {locale === "vi"
              ? "Thay đổi sẽ được áp dụng cho lần dự đoán tiếp theo."
              : "Changes apply to the next prediction."}
          </p>
        </form>
      </div>
    </main>
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

      <div className="space-y-5 rounded-[22px] bg-white p-5 shadow-[0_5px_20px_rgba(0,0,0,0.05)]">{children}</div>
    </section>
  );
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
