import type { OnboardingStep } from "@/types/auth";

import type { DiscordConnectionStatus } from "@/types/discord";

import type { TelegramConnectionStatus } from "@/types/telegram";

export type OnboardingState = {
  step: OnboardingStep;
  completedAt: string | null;
  mostRecentPeriodStartDate: string | null;
  defaultCycleLength: number;
  reminderDaysBefore: number;
  notificationTime: string;
  timezone: string;

  telegramStatus: TelegramConnectionStatus;
  telegramConnected: boolean;
  telegramUsername: string | null;

  discordStatus: DiscordConnectionStatus;
  discordConnected: boolean;
  discordUsername: string | null;

  expectedNextPeriodDate: string | null;
  reminderDate: string | null;
};

export type OnboardingCycleRequest = {
  startDate: string;
  defaultCycleLength: number;
};

export type OnboardingReminderRequest = {
  reminderDaysBefore: number;
  notificationTime: string;
  timezone: string;
};

export type CompleteOnboardingRequest = {
  skipNotificationChannels: boolean;
};
