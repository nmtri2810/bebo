"use client";

import { useEffect, useState } from "react";

import { Check, HeartPulse } from "lucide-react";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

import { getCurrentUser } from "@/lib/api/auth-api";

import { useAuthStore } from "@/stores/auth-store";

export default function MePage() {
  const router = useRouter();

  const t = useTranslations("Profile");
  const common = useTranslations("Common");

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const setUser = useAuthStore((state) => state.setUser);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    const loadCurrentUser = async () => {
      try {
        const currentUser = await getCurrentUser(accessToken);

        if (!cancelled) {
          setUser(currentUser);
        }
      } catch {
        if (!cancelled) {
          clearSession();
          router.replace("/");
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void loadCurrentUser();

    return () => {
      cancelled = true;
    };
  }, [accessToken, clearSession, hasHydrated, router, setUser]);

  const handleLogout = () => {
    clearSession();
    router.replace("/");
  };

  if (!hasHydrated || isLoading) {
    return (
      <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
        <p className="text-sm text-[#8e8e93]">{common("loading")}</p>
      </main>
    );
  }

  if (!accessToken) {
    return null;
  }

  return (
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-8">
      <div className="mx-auto w-full max-w-md">
        <div className="mb-6 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="flex size-9 items-center justify-center rounded-xl bg-[#ff2d55]">
              <HeartPulse className="size-5 text-white" />
            </div>

            <span className="text-lg font-bold tracking-[-0.02em]">bebo</span>
          </div>

          <LanguageSwitcher />
        </div>

        <Card className="rounded-[28px] border-0 bg-white shadow-[0_2px_12px_rgba(0,0,0,0.06)]">
          <CardHeader className="items-center px-6 pb-5 pt-8 text-center">
            <div className="mb-3 flex size-14 items-center justify-center rounded-full bg-[#34c759]/15">
              <Check className="size-7 text-[#248a3d]" strokeWidth={3} />
            </div>

            <CardTitle className="text-[26px] font-bold tracking-[-0.03em] text-[#1c1c1e]">{t("title")}</CardTitle>

            <p className="text-[15px] text-[#636366]">{t("description")}</p>
          </CardHeader>

          <CardContent className="px-6">
            <p className="mb-2 px-1 text-xs font-semibold uppercase tracking-wide text-[#8e8e93]">{t("account")}</p>

            <div className="overflow-hidden rounded-2xl bg-[#f2f2f7]">
              <AccountRow label={t("email")} value={user?.email ?? "—"} />

              <div className="ml-4 border-t border-black/5" />

              <AccountRow label={t("timezone")} value={user?.timezone ?? "—"} />

              {user?.status && (
                <>
                  <div className="ml-4 border-t border-black/5" />

                  <AccountRow label={t("status")} value={user.status} />
                </>
              )}
            </div>
          </CardContent>

          <CardFooter className="px-6 pb-7 pt-6">
            <Button
              type="button"
              variant="ghost"
              onClick={handleLogout}
              className="h-12 w-full rounded-xl bg-[#ff3b30]/10 text-[16px] font-semibold text-[#d70015] hover:bg-[#ff3b30]/15 hover:text-[#d70015]"
            >
              {common("logout")}
            </Button>
          </CardFooter>
        </Card>
      </div>
    </main>
  );
}

type AccountRowProps = {
  label: string;
  value: string;
};

function AccountRow({ label, value }: AccountRowProps) {
  return (
    <div className="flex min-h-14 items-center justify-between gap-4 px-4 py-3">
      <span className="text-[15px] text-[#3a3a3c]">{label}</span>

      <span className="truncate text-right text-[15px] text-[#8e8e93]">{value}</span>
    </div>
  );
}
