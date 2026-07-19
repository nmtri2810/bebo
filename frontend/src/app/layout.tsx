import type { Metadata } from "next";
import { NextIntlClientProvider } from "next-intl";
import { getLocale } from "next-intl/server";

import { AuthHydrator } from "@/components/auth-hydrator";

import "./globals.css";

export const metadata: Metadata = {
  title: "bebo",
  description: "A little help to care better.",
};

type RootLayoutProps = Readonly<{
  children: React.ReactNode;
}>;

export default async function RootLayout({ children }: RootLayoutProps) {
  const locale = await getLocale();

  return (
    <html lang={locale}>
      <body>
        <NextIntlClientProvider>
          <AuthHydrator />
          {children}
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
