"use client";

import { HeartPulse } from "lucide-react";

import Link from "next/link";

import { useTranslations } from "next-intl";

import { AppNavigation } from "@/components/app-navigation";

import { LanguageSwitcher } from "@/components/language-switcher";

type AppShellProps = {
  children: React.ReactNode;

  /*
   * Chỉ kiểm soát chiều rộng nội dung trang.
   *
   * Header luôn sử dụng max-w-6xl để navigation
   * không bị ép theo chiều rộng của nội dung.
   */
  maxWidthClassName?: string;
};

export function AppShell({ children, maxWidthClassName = "max-w-6xl" }: AppShellProps) {
  const t = useTranslations("Navigation");

  return (
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-5 pb-[calc(6.5rem+env(safe-area-inset-bottom))] sm:px-6 md:py-7 md:pb-10 lg:px-8">
      <div className="mx-auto w-full max-w-6xl">
        <header className="mb-8 grid grid-cols-[minmax(0,1fr)_auto] items-center gap-4 md:mb-10 md:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
          <Link
            href="/dashboard"
            className="flex min-w-0 items-center gap-3 justify-self-start rounded-[16px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#007aff]/30"
          >
            <div className="flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-linear-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_6px_16px_rgba(255,45,85,0.22)]">
              <HeartPulse className="size-6 text-white" />
            </div>

            <div className="min-w-0">
              <p className="truncate text-xl font-bold leading-5 tracking-[-0.04em] text-[#1c1c1e]">bebo</p>

              <p className="mt-1 hidden whitespace-nowrap text-xs text-[#8e8e93] sm:block">{t("brandSubtitle")}</p>
            </div>
          </Link>

          <div className="hidden justify-self-center md:block">
            <AppNavigation variant="desktop" />
          </div>

          <div className="justify-self-end">
            <LanguageSwitcher />
          </div>
        </header>

        <div className={`mx-auto w-full ${maxWidthClassName}`}>{children}</div>
      </div>

      <AppNavigation variant="mobile" />
    </main>
  );
}
