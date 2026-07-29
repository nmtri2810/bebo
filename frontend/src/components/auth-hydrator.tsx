"use client";

import { useEffect } from "react";

import { ApiClientError } from "@/lib/api/api-client";

import { getCurrentUser } from "@/lib/api/user-api";

import { useAuthStore } from "@/stores/auth-store";

export function AuthHydrator() {
  useEffect(() => {
    const rehydrate = async () => {
      try {
        await useAuthStore.persist.rehydrate();

        const { accessToken } = useAuthStore.getState();

        if (accessToken) {
          try {
            const currentUser = await getCurrentUser(accessToken);

            useAuthStore.getState().setUser(currentUser);
          } catch (error) {
            if (error instanceof ApiClientError && error.status === 401) {
              useAuthStore.getState().clearSession();
            }
          }
        }
      } finally {
        useAuthStore.getState().setHasHydrated(true);
      }
    };

    void rehydrate();
  }, []);

  return null;
}
