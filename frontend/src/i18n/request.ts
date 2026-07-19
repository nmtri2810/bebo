import { cookies } from "next/headers";
import { getRequestConfig } from "next-intl/server";

import enMessages from "../../messages/en.json";
import viMessages from "../../messages/vi.json";

const supportedLocales = ["en", "vi"] as const;

type SupportedLocale = (typeof supportedLocales)[number];

const messagesByLocale = {
  en: enMessages,
  vi: viMessages,
} satisfies Record<SupportedLocale, typeof enMessages>;

function isSupportedLocale(locale: string | undefined): locale is SupportedLocale {
  return supportedLocales.includes(locale as SupportedLocale);
}

export default getRequestConfig(async () => {
  const cookieStore = await cookies();

  const requestedLocale = cookieStore.get("locale")?.value;

  const locale: SupportedLocale = isSupportedLocale(requestedLocale) ? requestedLocale : "en";

  return {
    locale,
    messages: messagesByLocale[locale],
  };
});
