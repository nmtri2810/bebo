"use client";

import type { LucideIcon } from "lucide-react";

import { BellRing, LayoutDashboard, Settings2 } from "lucide-react";

import Link from "next/link";

import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";

type AppNavigationProps = {
  variant: "desktop" | "mobile";
};

type NavigationItem = {
  href: string;
  label: string;
  icon: LucideIcon;
};

function isRouteActive(pathname: string, href: string): boolean {
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function AppNavigation({ variant }: AppNavigationProps) {
  const pathname = usePathname();
  const t = useTranslations("Navigation");

  const items: NavigationItem[] = [
    {
      href: "/dashboard",
      label: t("overview"),
      icon: LayoutDashboard,
    },
    {
      href: "/notifications",
      label: t("notifications"),
      icon: BellRing,
    },
    {
      href: "/settings",
      label: t("settings"),
      icon: Settings2,
    },
  ];

  if (variant === "desktop") {
    return (
      <nav
        aria-label={t("primaryNavigation")}
        className="flex w-max shrink-0 items-center gap-1.5 rounded-full border border-black/6 bg-white/90 p-1.5 shadow-[0_5px_18px_rgba(0,0,0,0.055)] backdrop-blur-xl"
      >
        {items.map((item) => {
          const active = isRouteActive(pathname, item.href);

          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={
                active
                  ? "inline-flex h-10 shrink-0 items-center gap-2 rounded-full bg-[#ff2d55]/10 px-5 text-sm font-semibold text-[#d7003a] transition"
                  : "inline-flex h-10 shrink-0 items-center gap-2 rounded-full px-5 text-sm font-medium text-[#636366] transition hover:bg-black/4 hover:text-[#1c1c1e]"
              }
            >
              <Icon className="size-4 shrink-0" strokeWidth={active ? 2.4 : 2} />

              <span className="whitespace-nowrap">{item.label}</span>
            </Link>
          );
        })}
      </nav>
    );
  }

  return (
    <nav
      aria-label={t("primaryNavigation")}
      className="fixed inset-x-0 bottom-0 z-40 border-t border-black/6 bg-white/92 pb-[env(safe-area-inset-bottom)] backdrop-blur-xl md:hidden"
    >
      <div className="mx-auto grid max-w-md grid-cols-3 gap-1 px-2 py-2">
        {items.map((item) => {
          const active = isRouteActive(pathname, item.href);

          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={
                active
                  ? "flex min-h-14 min-w-0 flex-col items-center justify-center gap-1 rounded-[14px] bg-[#ff2d55]/10 px-2 text-[#d7003a] transition"
                  : "flex min-h-14 min-w-0 flex-col items-center justify-center gap-1 rounded-[14px] px-2 text-[#8e8e93] transition active:bg-black/5"
              }
            >
              <Icon className="size-5 shrink-0" strokeWidth={active ? 2.4 : 2} />

              <span className="max-w-full truncate text-[11px] font-semibold">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
