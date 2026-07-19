"use client";

import { Globe2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

export function LanguageSwitcher() {
  const locale = useLocale();
  const t = useTranslations("Common");
  const router = useRouter();

  const nextLocale = locale === "en" ? "vi" : "en";

  const nextLanguage = nextLocale === "en" ? t("english") : t("vietnamese");

  const changeLanguage = () => {
    document.cookie = [`locale=${nextLocale}`, "path=/", "max-age=31536000", "samesite=lax"].join("; ");

    router.refresh();
  };

  return (
    <button
      type="button"
      onClick={changeLanguage}
      aria-label={t("switchLanguage", {
        language: nextLanguage,
      })}
      className="inline-flex h-9 items-center gap-1.5 rounded-full bg-white px-3 text-sm font-medium text-[#3a3a3c] shadow-sm transition active:scale-95"
    >
      <Globe2 className="size-4 text-[#007aff]" />

      <span>{locale === "en" ? "EN" : "VI"}</span>
    </button>
  );
}
