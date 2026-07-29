import { apiRequest } from "@/lib/api/api-client";

import type { AuthUser } from "@/types/auth";

import type {
  CompleteOnboardingRequest,
  OnboardingCycleRequest,
  OnboardingReminderRequest,
  OnboardingState,
} from "@/types/onboarding";

export function getCurrentUser(accessToken: string): Promise<AuthUser> {
  return apiRequest<AuthUser>("/api/users/me", {
    method: "GET",
    token: accessToken,
  });
}

export function getOnboardingState(accessToken: string): Promise<OnboardingState> {
  return apiRequest<OnboardingState>("/api/users/me/onboarding", {
    method: "GET",
    token: accessToken,
  });
}

export function startOnboarding(accessToken: string): Promise<OnboardingState> {
  return apiRequest<OnboardingState>("/api/users/me/onboarding/start", {
    method: "POST",
    token: accessToken,
  });
}

export function saveOnboardingCycle(accessToken: string, request: OnboardingCycleRequest): Promise<OnboardingState> {
  return apiRequest<OnboardingState>("/api/users/me/onboarding/cycle", {
    method: "PUT",
    token: accessToken,
    body: JSON.stringify(request),
  });
}

export function saveOnboardingReminder(
  accessToken: string,
  request: OnboardingReminderRequest,
): Promise<OnboardingState> {
  return apiRequest<OnboardingState>("/api/users/me/onboarding/reminder", {
    method: "PUT",
    token: accessToken,
    body: JSON.stringify(request),
  });
}

export function completeOnboarding(accessToken: string, request: CompleteOnboardingRequest): Promise<OnboardingState> {
  return apiRequest<OnboardingState>("/api/users/me/onboarding/complete", {
    method: "POST",
    token: accessToken,
    body: JSON.stringify(request),
  });
}
