"use client";

import { type FormEvent, useCallback, useEffect, useState } from "react";

import {
  ArrowLeft,
  ArrowRight,
  BellRing,
  CalendarDays,
  Check,
  CheckCircle2,
  Clock3,
  Gamepad2,
  HeartHandshake,
  HeartPulse,
  Send,
  ShieldCheck,
  Sparkles,
} from "lucide-react";

import { useLocale, useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";

import { Button } from "@/components/ui/button";

import { Input } from "@/components/ui/input";

import { Label } from "@/components/ui/label";

import { DiscordConnectionCard } from "@/features/discord/components/discord-connection-card";

import { TelegramConnectionCard } from "@/features/telegram/components/telegram-connection-card";

import { ApiClientError } from "@/lib/api/api-client";

import {
  completeOnboarding,
  getOnboardingState,
  saveOnboardingCycle,
  saveOnboardingReminder,
  startOnboarding,
} from "@/lib/api/user-api";

import { useAuthStore } from "@/stores/auth-store";

import type { OnboardingStep } from "@/types/auth";

import type { DiscordConnection } from "@/types/discord";

import type { OnboardingState } from "@/types/onboarding";

import type { TelegramConnection } from "@/types/telegram";

type VisibleStep = Exclude<OnboardingStep, "COMPLETED">;

type CycleForm = {
  startDate: string;
  defaultCycleLength: string;
};

type ReminderForm = {
  reminderDaysBefore: string;
  notificationTime: string;
  timezone: string;
};

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

function formatLocalDate(value: string, locale: string): string {
  const [year, month, day] = value.split("-").map(Number);

  return new Intl.DateTimeFormat(locale.startsWith("vi") ? "vi-VN" : "en-US", {
    dateStyle: "long",
  }).format(new Date(year, month - 1, day));
}

function subtractDays(value: string, days: number): string {
  const [year, month, day] = value.split("-").map(Number);

  const date = new Date(year, month - 1, day);

  date.setDate(date.getDate() - days);

  const resultYear = date.getFullYear();

  const resultMonth = String(date.getMonth() + 1).padStart(2, "0");

  const resultDay = String(date.getDate()).padStart(2, "0");

  return `${resultYear}-` + `${resultMonth}-` + resultDay;
}

export default function OnboardingPage() {
  const router = useRouter();
  const locale = useLocale();

  const t = useTranslations("Onboarding");

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const setUser = useAuthStore((state) => state.setUser);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [onboardingState, setOnboardingState] = useState<OnboardingState | null>(null);

  const [visibleStep, setVisibleStep] = useState<VisibleStep>("WELCOME");

  const [cycleForm, setCycleForm] = useState<CycleForm>({
    startDate: "",
    defaultCycleLength: "28",
  });

  const [reminderForm, setReminderForm] = useState<ReminderForm>({
    reminderDaysBefore: "3",
    notificationTime: "08:00",
    timezone: "Asia/Ho_Chi_Minh",
  });

  const [isChecking, setIsChecking] = useState(true);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [showCompletion, setShowCompletion] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const today = getTodayInputValue();

  const applyState = useCallback((nextState: OnboardingState, moveToServerStep = true) => {
    setOnboardingState(nextState);

    setCycleForm({
      startDate: nextState.mostRecentPeriodStartDate ?? "",

      defaultCycleLength: String(nextState.defaultCycleLength),
    });

    setReminderForm({
      reminderDaysBefore: String(nextState.reminderDaysBefore),

      notificationTime: nextState.notificationTime,

      timezone: nextState.timezone,
    });

    if (moveToServerStep && nextState.step !== "COMPLETED") {
      setVisibleStep(nextState.step);
    }
  }, []);

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    getOnboardingState(accessToken)
      .then((result) => {
        if (cancelled) {
          return;
        }

        if (result.step === "COMPLETED") {
          const currentUser = useAuthStore.getState().user;

          if (currentUser) {
            setUser({
              ...currentUser,
              timezone: result.timezone,
              onboardingStep: "COMPLETED",
              onboardingCompletedAt: result.completedAt,
            });
          }

          router.replace("/dashboard");

          return;
        }

        applyState(result);
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

        setErrorMessage(getErrorMessage(error, t("genericError")));
      })
      .finally(() => {
        if (!cancelled) {
          setIsChecking(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, applyState, clearSession, hasHydrated, router, setUser, t]);

  const handleUnauthorized = (error: unknown): boolean => {
    if (error instanceof ApiClientError && error.status === 401) {
      clearSession();
      router.replace("/");

      return true;
    }

    return false;
  };

  const handleStart = async () => {
    if (!accessToken) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const result = await startOnboarding(accessToken);

      applyState(result, false);

      setVisibleStep("CYCLE");
    } catch (error) {
      if (!handleUnauthorized(error)) {
        setErrorMessage(getErrorMessage(error, t("genericError")));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCycleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken || !cycleForm.startDate) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const result = await saveOnboardingCycle(accessToken, {
        startDate: cycleForm.startDate,

        defaultCycleLength: Number(cycleForm.defaultCycleLength),
      });

      applyState(result, false);

      setVisibleStep("REMINDER");
    } catch (error) {
      if (!handleUnauthorized(error)) {
        setErrorMessage(getErrorMessage(error, t("cycleSaveError")));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReminderSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!accessToken) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const result = await saveOnboardingReminder(accessToken, {
        reminderDaysBefore: Number(reminderForm.reminderDaysBefore),

        notificationTime: reminderForm.notificationTime,

        timezone: reminderForm.timezone.trim(),
      });

      applyState(result, false);

      setVisibleStep("CHANNELS");
    } catch (error) {
      if (!handleUnauthorized(error)) {
        setErrorMessage(getErrorMessage(error, t("reminderSaveError")));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleTelegramConnectionChange = (connection: TelegramConnection) => {
    setOnboardingState((current) => {
      if (!current) {
        return current;
      }

      return {
        ...current,
        telegramStatus: connection.status,
        telegramConnected: connection.connected,
        telegramUsername: connection.telegramUsername,
      };
    });
  };

  const handleDiscordConnectionChange = (connection: DiscordConnection) => {
    setOnboardingState((current) => {
      if (!current) {
        return current;
      }

      return {
        ...current,
        discordStatus: connection.status,
        discordConnected: connection.connected,
        discordUsername: connection.discordUsername,
      };
    });
  };

  const handleComplete = async (skipNotificationChannels: boolean) => {
    if (!accessToken) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const result = await completeOnboarding(accessToken, {
        skipNotificationChannels,
      });

      applyState(result, false);

      if (user) {
        setUser({
          ...user,
          timezone: result.timezone,
          onboardingStep: "COMPLETED",
          onboardingCompletedAt: result.completedAt,
        });
      }

      setShowCompletion(true);
    } catch (error) {
      if (!handleUnauthorized(error)) {
        setErrorMessage(getErrorMessage(error, t("completeError")));
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!hasHydrated || isChecking) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <div className="flex flex-col items-center gap-3">
          <div className="size-6 animate-spin rounded-full border-2 border-[#007aff]/20 border-t-[#007aff]" />

          <p className="text-sm text-[#8e8e93]">{t("loading")}</p>
        </div>
      </main>
    );
  }

  if (!accessToken || !onboardingState) {
    return null;
  }

  return (
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-5 sm:px-6 sm:py-8">
      <div className="mx-auto w-full max-w-5xl">
        <header className="mb-6 flex items-center justify-between sm:mb-8">
          <div className="flex items-center gap-3">
            <div className="flex size-11 items-center justify-center rounded-[14px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_6px_16px_rgba(255,45,85,0.22)]">
              <HeartPulse className="size-6 text-white" />
            </div>

            <div>
              <p className="text-xl font-bold leading-5 tracking-[-0.04em] text-[#1c1c1e]">bebo</p>

              <p className="mt-1 text-xs text-[#8e8e93]">Better Boyfriend</p>
            </div>
          </div>

          <LanguageSwitcher />
        </header>

        {!showCompletion && visibleStep !== "WELCOME" && <OnboardingProgress currentStep={visibleStep} />}

        {errorMessage && (
          <div
            role="alert"
            className="mx-auto mb-5 max-w-2xl rounded-[16px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]"
          >
            {errorMessage}
          </div>
        )}

        {showCompletion ? (
          <CompletionStep state={onboardingState} locale={locale} onContinue={() => router.replace("/dashboard")} />
        ) : visibleStep === "WELCOME" ? (
          <WelcomeStep
            isSubmitting={isSubmitting}
            onStart={() => {
              void handleStart();
            }}
          />
        ) : visibleStep === "CYCLE" ? (
          <CycleStep
            form={cycleForm}
            today={today}
            isSubmitting={isSubmitting}
            onChange={setCycleForm}
            onBack={() => setVisibleStep("WELCOME")}
            onSubmit={handleCycleSubmit}
          />
        ) : visibleStep === "REMINDER" ? (
          <ReminderStep
            form={reminderForm}
            expectedNextPeriodDate={onboardingState.expectedNextPeriodDate}
            locale={locale}
            isSubmitting={isSubmitting}
            onChange={setReminderForm}
            onBack={() => setVisibleStep("CYCLE")}
            onSubmit={handleReminderSubmit}
          />
        ) : (
          <ChannelsStep
            accessToken={accessToken}
            state={onboardingState}
            isSubmitting={isSubmitting}
            onTelegramConnectionChange={handleTelegramConnectionChange}
            onDiscordConnectionChange={handleDiscordConnectionChange}
            onBack={() => setVisibleStep("REMINDER")}
            onComplete={() => {
              void handleComplete(false);
            }}
            onSkip={() => {
              void handleComplete(true);
            }}
          />
        )}
      </div>
    </main>
  );
}

type OnboardingProgressProps = {
  currentStep: Exclude<VisibleStep, "WELCOME">;
};

function OnboardingProgress({ currentStep }: OnboardingProgressProps) {
  const t = useTranslations("Onboarding");

  const steps = [
    {
      id: "CYCLE" as const,
      label: t("progressCycle"),
    },
    {
      id: "REMINDER" as const,
      label: t("progressReminder"),
    },
    {
      id: "CHANNELS" as const,
      label: t("progressChannels"),
    },
  ];

  const currentIndex = steps.findIndex((step) => step.id === currentStep);

  return (
    <div className="mx-auto mb-6 max-w-2xl rounded-[20px] bg-white px-5 py-4 shadow-[0_5px_20px_rgba(0,0,0,0.04)] sm:mb-8 sm:px-6">
      <div className="flex items-center justify-between gap-3">
        {steps.map((step, index) => {
          const active = index === currentIndex;

          const completed = index < currentIndex;

          return (
            <div key={step.id} className="flex shrink-0 items-center gap-2">
              <div
                className={
                  active || completed
                    ? "flex size-8 shrink-0 items-center justify-center rounded-full bg-[#ff2d55] text-white"
                    : "flex size-8 shrink-0 items-center justify-center rounded-full bg-[#f2f2f7] text-[#8e8e93]"
                }
              >
                {completed ? <Check className="size-4" /> : <span className="text-xs font-bold">{index + 1}</span>}
              </div>

              <span
                className={
                  active
                    ? "whitespace-nowrap text-xs font-semibold text-[#1c1c1e]"
                    : "whitespace-nowrap text-xs text-[#8e8e93]"
                }
              >
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

type WelcomeStepProps = {
  isSubmitting: boolean;
  onStart: () => void;
};

function WelcomeStep({ isSubmitting, onStart }: WelcomeStepProps) {
  const t = useTranslations("Onboarding");

  const benefits = [
    {
      icon: CalendarDays,
      title: t("welcomeBenefitCycle"),
      description: t("welcomeBenefitCycleHint"),
    },
    {
      icon: BellRing,
      title: t("welcomeBenefitReminder"),
      description: t("welcomeBenefitReminderHint"),
    },
    {
      icon: HeartHandshake,
      title: t("welcomeBenefitCare"),
      description: t("welcomeBenefitCareHint"),
    },
  ];

  return (
    <section className="overflow-hidden rounded-[30px] bg-white shadow-[0_14px_45px_rgba(0,0,0,0.07)] lg:grid lg:grid-cols-[1.05fr_0.95fr]">
      <div className="bg-linear-to-br from-[#ff375f] to-[#ff2d55] p-7 text-white sm:p-10 lg:p-12">
        <div className="flex size-15 items-center justify-center rounded-[18px] bg-white/18 backdrop-blur">
          <Sparkles className="size-8" />
        </div>

        <p className="mt-8 text-sm font-semibold uppercase tracking-[0.09em] text-white/75">{t("welcomeEyebrow")}</p>

        <h1 className="mt-3 max-w-lg text-[38px] font-bold leading-[1.04] tracking-[-0.055em] sm:text-[48px]">
          {t("welcomeTitle")}
        </h1>

        <p className="mt-5 max-w-lg text-[16px] leading-7 text-white/82">{t("welcomeDescription")}</p>
      </div>

      <div className="p-6 sm:p-9 lg:p-10">
        <div className="space-y-5">
          {benefits.map((benefit) => {
            const Icon = benefit.icon;

            return (
              <div key={benefit.title} className="flex gap-4">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#ff2d55]/10">
                  <Icon className="size-5 text-[#d7003a]" />
                </div>

                <div>
                  <h2 className="text-[16px] font-semibold text-[#1c1c1e]">{benefit.title}</h2>

                  <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{benefit.description}</p>
                </div>
              </div>
            );
          })}
        </div>

        <Button
          type="button"
          onClick={onStart}
          disabled={isSubmitting}
          className="mt-8 h-12 w-full rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
        >
          {isSubmitting ? t("saving") : t("welcomeCta")}

          {!isSubmitting && <ArrowRight className="size-4" />}
        </Button>

        <div className="mt-4 flex items-start justify-center gap-2 text-center text-xs leading-5 text-[#8e8e93]">
          <ShieldCheck className="mt-0.5 size-4 shrink-0" />

          <span>{t("privacy")}</span>
        </div>
      </div>
    </section>
  );
}

type CycleStepProps = {
  form: CycleForm;
  today: string;
  isSubmitting: boolean;
  onChange: (form: CycleForm) => void;
  onBack: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

function CycleStep({ form, today, isSubmitting, onChange, onBack, onSubmit }: CycleStepProps) {
  const t = useTranslations("Onboarding");

  return (
    <StepCard
      icon={<CalendarDays className="size-7 text-white" />}
      iconClassName="from-[#ff375f] to-[#ff2d55]"
      eyebrow={t("cycleEyebrow")}
      title={t("cycleTitle")}
      description={t("cycleDescription")}
    >
      <form className="space-y-5" onSubmit={onSubmit}>
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
              value={form.startDate}
              onChange={(event) =>
                onChange({
                  ...form,
                  startDate: event.target.value,
                })
              }
              className="h-13 rounded-[14px] border-0 bg-[#f2f2f7] pl-12 text-[16px] shadow-none"
              required
            />
          </div>

          <p className="px-1 text-xs leading-5 text-[#8e8e93]">{t("dateHint")}</p>
        </div>

        <div className="space-y-2">
          <Label htmlFor="defaultCycleLength" className="text-[13px] font-semibold text-[#3a3a3c]">
            {t("defaultCycleLength")}
          </Label>

          <div className="relative">
            <Input
              id="defaultCycleLength"
              name="defaultCycleLength"
              type="number"
              min={15}
              max={60}
              value={form.defaultCycleLength}
              onChange={(event) =>
                onChange({
                  ...form,
                  defaultCycleLength: event.target.value,
                })
              }
              className="h-13 rounded-[14px] border-0 bg-[#f2f2f7] pr-16 text-[16px] shadow-none"
              required
            />

            <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-[#8e8e93]">
              {t("days")}
            </span>
          </div>

          <p className="px-1 text-xs leading-5 text-[#8e8e93]">{t("defaultCycleLengthHint")}</p>
        </div>

        <StepActions isSubmitting={isSubmitting} onBack={onBack} continueLabel={t("continue")} />
      </form>
    </StepCard>
  );
}

type ReminderStepProps = {
  form: ReminderForm;
  expectedNextPeriodDate: string | null;
  locale: string;
  isSubmitting: boolean;
  onChange: (form: ReminderForm) => void;
  onBack: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

function ReminderStep({
  form,
  expectedNextPeriodDate,
  locale,
  isSubmitting,
  onChange,
  onBack,
  onSubmit,
}: ReminderStepProps) {
  const t = useTranslations("Onboarding");

  const reminderDays = Number(form.reminderDaysBefore || 0);

  const previewReminderDate = expectedNextPeriodDate ? subtractDays(expectedNextPeriodDate, reminderDays) : null;

  return (
    <StepCard
      icon={<BellRing className="size-7 text-white" />}
      iconClassName="from-[#5856d6] to-[#007aff]"
      eyebrow={t("reminderEyebrow")}
      title={t("reminderTitle")}
      description={t("reminderDescription")}
    >
      <form className="space-y-5" onSubmit={onSubmit}>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="reminderDaysBefore" className="text-[13px] font-semibold text-[#3a3a3c]">
              {t("reminderDaysBefore")}
            </Label>

            <div className="relative">
              <Input
                id="reminderDaysBefore"
                type="number"
                min={0}
                max={14}
                value={form.reminderDaysBefore}
                onChange={(event) =>
                  onChange({
                    ...form,
                    reminderDaysBefore: event.target.value,
                  })
                }
                className="h-13 rounded-[14px] border-0 bg-[#f2f2f7] pr-16 text-[16px] shadow-none"
                required
              />

              <span className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-sm text-[#8e8e93]">
                {t("days")}
              </span>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="notificationTime" className="text-[13px] font-semibold text-[#3a3a3c]">
              {t("notificationTime")}
            </Label>

            <div className="relative">
              <Clock3 className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-[#007aff]" />

              <Input
                id="notificationTime"
                type="time"
                value={form.notificationTime}
                onChange={(event) =>
                  onChange({
                    ...form,
                    notificationTime: event.target.value,
                  })
                }
                className="h-13 rounded-[14px] border-0 bg-[#f2f2f7] pl-12 text-[16px] shadow-none"
                required
              />
            </div>
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="timezone" className="text-[13px] font-semibold text-[#3a3a3c]">
            {t("timezone")}
          </Label>

          <Input
            id="timezone"
            type="text"
            value={form.timezone}
            onChange={(event) =>
              onChange({
                ...form,
                timezone: event.target.value,
              })
            }
            className="h-13 rounded-[14px] border-0 bg-[#f2f2f7] text-[16px] shadow-none"
            required
          />

          <p className="px-1 text-xs leading-5 text-[#8e8e93]">{t("timezoneHint")}</p>
        </div>

        <div className="rounded-[18px] bg-[#5856d6]/8 p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.06em] text-[#5856d6]">{t("previewTitle")}</p>

          <p className="mt-2 text-sm font-semibold text-[#1c1c1e]">
            {t("previewMessage", {
              count: Number(form.reminderDaysBefore || 0),
            })}
          </p>

          {previewReminderDate && (
            <p className="mt-1 text-xs text-[#8e8e93]">
              {formatLocalDate(previewReminderDate, locale)}
              {" · "}
              {form.notificationTime}
              {" · "}
              {form.timezone}
            </p>
          )}
        </div>

        <StepActions isSubmitting={isSubmitting} onBack={onBack} continueLabel={t("continue")} />
      </form>
    </StepCard>
  );
}

type ChannelsStepProps = {
  accessToken: string;
  state: OnboardingState;
  isSubmitting: boolean;

  onTelegramConnectionChange: (connection: TelegramConnection) => void;

  onDiscordConnectionChange: (connection: DiscordConnection) => void;

  onBack: () => void;
  onComplete: () => void;
  onSkip: () => void;
};

function ChannelsStep({
  accessToken,
  state,
  isSubmitting,
  onTelegramConnectionChange,
  onDiscordConnectionChange,
  onBack,
  onComplete,
  onSkip,
}: ChannelsStepProps) {
  const t = useTranslations("Onboarding");

  const hasConnectedChannel = state.telegramConnected || state.discordConnected;

  return (
    <StepCard
      icon={<BellRing className="size-7 text-white" />}
      iconClassName="from-[#5865f2] to-[#229ed9]"
      eyebrow={t("channelsEyebrow")}
      title={t("channelsTitle")}
      description={t("channelsDescription")}
    >
      <div className="rounded-[16px] bg-[#f2f2f7] px-4 py-3 text-sm leading-5 text-[#636366]">
        {t("channelsChoiceHint")}
      </div>

      <div className="mt-5 space-y-4">
        <TelegramConnectionCard
          accessToken={accessToken}
          showSectionTitle={false}
          allowDisconnect={false}
          onConnectionChange={onTelegramConnectionChange}
        />

        <DiscordConnectionCard
          accessToken={accessToken}
          showSectionTitle={false}
          allowDisconnect={false}
          callbackReturnPath="/onboarding"
          onConnectionChange={onDiscordConnectionChange}
        />
      </div>

      <div className="mt-5 rounded-[16px] bg-[#f2f2f7] px-4 py-3 text-xs leading-5 text-[#636366]">
        <ShieldCheck className="mr-2 inline size-4 text-[#34c759]" />

        {t("channelsPrivacy")}
      </div>

      <div className="mt-6 grid gap-3 sm:grid-cols-[auto_1fr]">
        <Button
          type="button"
          variant="ghost"
          onClick={onBack}
          disabled={isSubmitting}
          className="h-12 rounded-[14px] px-5 text-[#636366]"
        >
          <ArrowLeft className="size-4" />

          {t("back")}
        </Button>

        {hasConnectedChannel ? (
          <Button
            type="button"
            onClick={onComplete}
            disabled={isSubmitting}
            className="h-12 rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
          >
            {isSubmitting ? t("saving") : t("finishSetup")}

            {!isSubmitting && <ArrowRight className="size-4" />}
          </Button>
        ) : (
          <Button
            type="button"
            variant="ghost"
            onClick={onSkip}
            disabled={isSubmitting}
            className="h-12 rounded-[14px] text-[#007aff] hover:bg-[#007aff]/8 hover:text-[#007aff]"
          >
            {isSubmitting ? t("saving") : t("skipChannels")}
          </Button>
        )}
      </div>

      {!hasConnectedChannel && (
        <p className="mt-3 text-center text-xs leading-5 text-[#8e8e93]">{t("skipChannelsHint")}</p>
      )}
    </StepCard>
  );
}

type CompletionStepProps = {
  state: OnboardingState;
  locale: string;
  onContinue: () => void;
};

function CompletionStep({ state, locale, onContinue }: CompletionStepProps) {
  const t = useTranslations("Onboarding");

  return (
    <section className="mx-auto max-w-2xl overflow-hidden rounded-[30px] bg-white text-center shadow-[0_14px_45px_rgba(0,0,0,0.07)]">
      <div className="bg-linear-to-br from-[#34c759] to-[#30b45a] px-6 py-9 text-white">
        <div className="mx-auto flex size-17 items-center justify-center rounded-full bg-white/20">
          <CheckCircle2 className="size-9" />
        </div>

        <h1 className="mt-5 text-[34px] font-bold tracking-[-0.045em]">{t("completeTitle")}</h1>

        <p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-white/82">{t("completeDescription")}</p>
      </div>

      <div className="p-6 text-left sm:p-8">
        <div className="overflow-hidden rounded-[20px] bg-[#f2f2f7]">
          <SummaryRow
            icon={<CalendarDays className="size-5 text-[#ff2d55]" />}
            label={t("completeNextPeriod")}
            value={
              state.expectedNextPeriodDate ? formatLocalDate(state.expectedNextPeriodDate, locale) : t("notAvailable")
            }
          />

          <div className="ml-14 border-t border-black/6" />

          <SummaryRow
            icon={<BellRing className="size-5 text-[#5856d6]" />}
            label={t("completeReminder")}
            value={
              state.reminderDate
                ? `${formatLocalDate(state.reminderDate, locale)} · ${state.notificationTime}`
                : t("notAvailable")
            }
          />

          <div className="ml-14 border-t border-black/6" />

          <SummaryRow
            icon={<Send className="size-5 text-[#229ed9]" />}
            label={t("completeTelegram")}
            value={
              state.telegramConnected
                ? state.telegramUsername
                  ? `@${state.telegramUsername}`
                  : t("connected")
                : t("notConnected")
            }
          />

          <div className="ml-14 border-t border-black/6" />

          <SummaryRow
            icon={<Gamepad2 className="size-5 text-[#5865f2]" />}
            label={t("completeDiscord")}
            value={state.discordConnected ? (state.discordUsername ?? t("connected")) : t("notConnected")}
          />
        </div>

        <Button
          type="button"
          onClick={onContinue}
          className="mt-6 h-12 w-full rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
        >
          {t("goToDashboard")}

          <ArrowRight className="size-4" />
        </Button>
      </div>
    </section>
  );
}

type StepCardProps = {
  icon: React.ReactNode;
  iconClassName: string;
  eyebrow: string;
  title: string;
  description: string;
  children: React.ReactNode;
};

function StepCard({ icon, iconClassName, eyebrow, title, description, children }: StepCardProps) {
  return (
    <section className="mx-auto max-w-2xl rounded-[30px] bg-white p-6 shadow-[0_14px_45px_rgba(0,0,0,0.07)] sm:p-8">
      <div className="flex items-start gap-4">
        <div
          className={`flex size-14 shrink-0 items-center justify-center rounded-[18px] bg-linear-to-br ${iconClassName}`}
        >
          {icon}
        </div>

        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8e8e93]">{eyebrow}</p>

          <h1 className="mt-1 text-[28px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e] sm:text-[32px]">
            {title}
          </h1>

          <p className="mt-2 text-sm leading-6 text-[#636366]">{description}</p>
        </div>
      </div>

      <div className="mt-7">{children}</div>
    </section>
  );
}

type StepActionsProps = {
  isSubmitting: boolean;
  onBack: () => void;
  continueLabel: string;
};

function StepActions({ isSubmitting, onBack, continueLabel }: StepActionsProps) {
  const t = useTranslations("Onboarding");

  return (
    <div className="grid gap-3 pt-1 sm:grid-cols-[auto_1fr]">
      <Button
        type="button"
        variant="ghost"
        onClick={onBack}
        disabled={isSubmitting}
        className="h-12 rounded-[14px] px-5 text-[#636366]"
      >
        <ArrowLeft className="size-4" />

        {t("back")}
      </Button>

      <Button
        type="submit"
        disabled={isSubmitting}
        className="h-12 rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
      >
        {isSubmitting ? t("saving") : continueLabel}

        {!isSubmitting && <ArrowRight className="size-4" />}
      </Button>
    </div>
  );
}

type SummaryRowProps = {
  icon: React.ReactNode;
  label: string;
  value: string;
};

function SummaryRow({ icon, label, value }: SummaryRowProps) {
  return (
    <div className="flex min-h-16 items-center gap-3 px-4 py-3">
      <div className="flex size-9 shrink-0 items-center justify-center rounded-[11px] bg-white">{icon}</div>

      <p className="flex-1 text-sm text-[#636366]">{label}</p>

      <p className="max-w-[55%] text-right text-sm font-semibold text-[#1c1c1e]">{value}</p>
    </div>
  );
}
