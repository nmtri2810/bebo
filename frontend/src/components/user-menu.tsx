"use client";

import { useEffect, useRef, useState } from "react";

import { ChevronDown, LogOut, UserRound } from "lucide-react";

import { useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { useAuthStore } from "@/stores/auth-store";

function getInitial(email: string): string {
  const initial = email.trim().charAt(0).toUpperCase();

  return initial || "U";
}

export function UserMenu() {
  const router = useRouter();

  const t = useTranslations("UserMenu");

  const containerRef = useRef<HTMLDivElement | null>(null);

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handlePointerDown = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handlePointerDown);

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("mousedown", handlePointerDown);

      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  if (!accessToken || !user) {
    return null;
  }

  const handleLogout = () => {
    setIsOpen(false);
    clearSession();
    router.replace("/");
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        aria-label={t("accountMenu")}
        onClick={() => setIsOpen((current) => !current)}
        className="inline-flex h-9 items-center gap-1 rounded-full bg-white p-1 pr-2 text-[#3a3a3c] shadow-sm transition hover:bg-[#fafafa] active:scale-95"
      >
        <span className="flex size-7 items-center justify-center rounded-full bg-[#1c1c1e] text-xs font-bold text-white">
          {user.email ? getInitial(user.email) : <UserRound className="size-4" />}
        </span>

        <ChevronDown
          className={isOpen ? "size-3.5 rotate-180 text-[#8e8e93] transition" : "size-3.5 text-[#8e8e93] transition"}
        />
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute right-0 top-[calc(100%+0.5rem)] z-50 w-[min(18rem,calc(100vw-2rem))] overflow-hidden rounded-[18px] border border-black/6 bg-white shadow-[0_14px_40px_rgba(0,0,0,0.14)]"
        >
          <div className="px-4 py-3">
            <p className="text-xs font-medium text-[#8e8e93]">{t("signedInAs")}</p>

            <p className="mt-1 truncate text-sm font-semibold text-[#1c1c1e]">{user.email}</p>
          </div>

          <div className="border-t border-black/6" />

          <button
            type="button"
            role="menuitem"
            onClick={handleLogout}
            className="flex min-h-12 w-full items-center gap-3 px-4 py-3 text-left text-sm font-semibold text-[#d70015] transition hover:bg-[#ff3b30]/6"
          >
            <div className="flex size-8 items-center justify-center rounded-[10px] bg-[#ff3b30]/10">
              <LogOut className="size-4" />
            </div>

            {t("signOut")}
          </button>
        </div>
      )}
    </div>
  );
}
