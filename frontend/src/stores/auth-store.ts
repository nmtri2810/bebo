"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";

import type { AuthUser } from "@/types/auth";

type AuthState = {
  accessToken: string | null;
  user: AuthUser | null;
  hasHydrated: boolean;

  setSession: (accessToken: string, user: AuthUser) => void;

  setUser: (user: AuthUser) => void;

  clearSession: () => void;

  setHasHydrated: (hasHydrated: boolean) => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      user: null,
      hasHydrated: false,

      setSession: (accessToken, user) => {
        set({
          accessToken,
          user,
        });
      },

      setUser: (user) => {
        set({ user });
      },

      clearSession: () => {
        set({
          accessToken: null,
          user: null,
        });
      },

      setHasHydrated: (hasHydrated) => {
        set({ hasHydrated });
      },
    }),
    {
      name: "bebo-auth",

      partialize: (state) => ({
        accessToken: state.accessToken,
        user: state.user,
      }),

      /*
       * Chủ động rehydrate sau khi browser mount,
       * tránh khác biệt state giữa server và client.
       */
      skipHydration: true,
    },
  ),
);
