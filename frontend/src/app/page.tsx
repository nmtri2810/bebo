"use client";

import { type FormEvent, useEffect, useState } from "react";

import { HeartPulse, LockKeyhole } from "lucide-react";

import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

import { login, register } from "@/lib/api/auth-api";

import { ApiClientError } from "@/lib/api/api-client";

import { useAuthStore } from "@/stores/auth-store";

type AuthMode = "login" | "register";

function getErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiClientError) {
    const firstFieldError = Object.values(error.fieldErrors)[0];

    return firstFieldError ?? error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

export default function AuthPage() {
  const router = useRouter();
  const t = useTranslations("Auth");

  const accessToken = useAuthStore((state) => state.accessToken);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const setSession = useAuthStore((state) => state.setSession);

  const [mode, setMode] = useState<AuthMode>("login");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (hasHydrated && accessToken) {
      router.replace("/me");
    }
  }, [accessToken, hasHydrated, router]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const normalizedEmail = email.trim().toLowerCase();

      const response =
        mode === "login"
          ? await login({
              email: normalizedEmail,
              password,
            })
          : await register({
              email: normalizedEmail,
              password,
              timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Ho_Chi_Minh",
            });

      setSession(response.accessToken, {
        id: response.userId,
        email: response.email,
        timezone: response.timezone,
      });

      router.replace("/me");
    } catch (error) {
      setErrorMessage(getErrorMessage(error, t("genericError")));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleModeChange = (value: string) => {
    if (value !== "login" && value !== "register") {
      return;
    }

    setMode(value);
    setErrorMessage(null);
  };

  const isLogin = mode === "login";

  return (
    <main className="flex min-h-dvh items-center justify-center bg-[#f2f2f7] px-4 py-12">
      <div className="relative w-full max-w-[420px]">
        <div className="absolute -top-12 right-0">
          <LanguageSwitcher />
        </div>

        <Card className="overflow-hidden rounded-[30px] border border-black/[0.06] bg-white shadow-[0_12px_40px_rgba(0,0,0,0.08)]">
          <CardHeader className="flex flex-col items-center px-6 pb-5 pt-8 text-center">
            <div className="mb-5 flex flex-col items-center">
              <div className="flex size-[68px] items-center justify-center rounded-[20px] bg-gradient-to-br from-[#ff375f] to-[#ff2d55] shadow-[0_8px_20px_rgba(255,45,85,0.25)]">
                <HeartPulse className="size-9 text-white" strokeWidth={2.2} />
              </div>

              <div className="mt-3 flex items-center gap-1.5">
                <span className="text-[26px] font-bold tracking-[-0.04em] text-[#1c1c1e]">bebo</span>

                <span className="mt-1 size-1.5 rounded-full bg-[#ff2d55]" />
              </div>

              <p className="mt-0.5 text-[13px] font-medium text-[#8e8e93]">Better Boyfriend</p>
            </div>

            <CardTitle className="text-[27px] font-bold tracking-[-0.035em] text-[#1c1c1e]">
              {isLogin ? t("welcomeBack") : t("createAccount")}
            </CardTitle>

            <CardDescription className="mt-1 max-w-[310px] text-[15px] leading-6 text-[#636366]">
              {isLogin ? t("loginDescription") : t("registerDescription")}
            </CardDescription>
          </CardHeader>

          <CardContent className="px-6 pb-7">
            <Tabs value={mode} onValueChange={handleModeChange}>
              <TabsList className="grid h-11 w-full grid-cols-2 rounded-[13px] bg-[#ededf2] p-1">
                <TabsTrigger
                  value="login"
                  className="rounded-[10px] text-sm font-medium text-[#636366] data-[state=active]:bg-white data-[state=active]:text-[#1c1c1e] data-[state=active]:shadow-sm"
                >
                  {t("loginTab")}
                </TabsTrigger>

                <TabsTrigger
                  value="register"
                  className="rounded-[10px] text-sm font-medium text-[#636366] data-[state=active]:bg-white data-[state=active]:text-[#1c1c1e] data-[state=active]:shadow-sm"
                >
                  {t("registerTab")}
                </TabsTrigger>
              </TabsList>
            </Tabs>

            <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="email" className="text-[13px] font-semibold text-[#3a3a3c]">
                  {t("email")}
                </Label>

                <Input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder={t("emailPlaceholder")}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] px-4 text-[16px] shadow-none placeholder:text-[#8e8e93] focus-visible:ring-2 focus-visible:ring-[#007aff]/30"
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-[13px] font-semibold text-[#3a3a3c]">
                  {t("password")}
                </Label>

                <Input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete={isLogin ? "current-password" : "new-password"}
                  placeholder={t("passwordPlaceholder")}
                  minLength={8}
                  maxLength={72}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] px-4 text-[16px] shadow-none placeholder:text-[#8e8e93] focus-visible:ring-2 focus-visible:ring-[#007aff]/30"
                  required
                />
              </div>

              {errorMessage && (
                <div role="alert" className="rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
                  {errorMessage}
                </div>
              )}

              <Button
                type="submit"
                disabled={isSubmitting || !hasHydrated}
                className="h-12 w-full rounded-[14px] bg-[#007aff] text-[16px] font-semibold text-white shadow-none transition hover:bg-[#006ee6] active:scale-[0.99]"
              >
                {isSubmitting ? t("processing") : isLogin ? t("signIn") : t("create")}
              </Button>

              <div className="flex items-center justify-center gap-1.5 pt-1 text-xs text-[#8e8e93]">
                <LockKeyhole className="size-3.5" />
                <span>{t("privacy")}</span>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
