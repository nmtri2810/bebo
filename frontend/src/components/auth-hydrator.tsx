"use client";

import { useEffect } from "react";

import { useAuthStore } from "@/stores/auth-store";

export function AuthHydrator() {
  useEffect(() => {
    const rehydrate = async () => {
      try {
        await useAuthStore.persist.rehydrate();
      } finally {
        useAuthStore.getState().setHasHydrated(true);
      }
    };

    void rehydrate();
  }, []);

  return null;
}
