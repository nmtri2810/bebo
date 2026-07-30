"use client";

import { useCallback, useEffect, useState } from "react";

import { Check, CircleAlert, ExternalLink, LoaderCircle, RefreshCw, Send, TestTube2, Unlink } from "lucide-react";

import { useTranslations } from "next-intl";

import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";

import { ApiClientError } from "@/lib/api/api-client";

import {
  beginTelegramConnection,
  disconnectTelegram,
  getTelegramConnection,
  sendTelegramTest,
} from "@/lib/api/telegram-api";

import { useAuthStore } from "@/stores/auth-store";

import type { TelegramConnection } from "@/types/telegram";

type TelegramConnectionCardProps = {
  accessToken: string;
  showSectionTitle?: boolean;
  allowDisconnect?: boolean;

  onConnectionChange?: (connection: TelegramConnection) => void;
};

export function TelegramConnectionCard({
  accessToken,
  showSectionTitle = true,
  allowDisconnect = true,
  onConnectionChange,
}: TelegramConnectionCardProps) {
  const router = useRouter();

  const t = useTranslations("Telegram");

  const clearSession = useAuthStore((state) => state.clearSession);

  const [connection, setConnection] = useState<TelegramConnection | null>(null);

  const [deepLink, setDeepLink] = useState<string | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [isConnecting, setIsConnecting] = useState(false);

  const [isDisconnecting, setIsDisconnecting] = useState(false);

  const [isTesting, setIsTesting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const handleUnauthorized = useCallback(() => {
    clearSession();
    router.replace("/");
  }, [clearSession, router]);

  const updateConnection = useCallback(
    (nextConnection: TelegramConnection) => {
      setConnection(nextConnection);

      onConnectionChange?.(nextConnection);
    },
    [onConnectionChange],
  );

  useEffect(() => {
    let cancelled = false;

    getTelegramConnection(accessToken)
      .then((result) => {
        if (!cancelled) {
          updateConnection(result);
        }
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        if (error instanceof ApiClientError && error.status === 401) {
          handleUnauthorized();

          return;
        }

        setErrorMessage(t("loadError"));
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, handleUnauthorized, t, updateConnection]);

  useEffect(() => {
    if (connection?.status !== "PENDING") {
      return;
    }

    const intervalId = window.setInterval(() => {
      getTelegramConnection(accessToken)
        .then((result) => {
          updateConnection(result);

          if (result.status === "CONNECTED" || result.status === "ALREADY_LINKED") {
            setDeepLink(null);
            setErrorMessage(null);
          }
        })
        .catch((error: unknown) => {
          if (error instanceof ApiClientError && error.status === 401) {
            handleUnauthorized();
          }
        });
    }, 2000);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [accessToken, connection?.status, handleUnauthorized, updateConnection]);

  const handleConnect = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsConnecting(true);

    try {
      const result = await beginTelegramConnection(accessToken);

      updateConnection({
        status: result.status,
        connected: false,
        telegramUsername: null,
        connectedAt: null,
      });

      setDeepLink(result.deepLink);

      window.open(result.deepLink, "_blank", "noopener,noreferrer");
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        handleUnauthorized();

        return;
      }

      setErrorMessage(t("connectError"));
    } finally {
      setIsConnecting(false);
    }
  };

  const handleTest = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsTesting(true);

    try {
      await sendTelegramTest(accessToken);

      setSuccessMessage(t("testSent"));
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        handleUnauthorized();

        return;
      }

      setErrorMessage(t("testError"));
    } finally {
      setIsTesting(false);
    }
  };

  const handleDisconnect = async () => {
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsDisconnecting(true);

    try {
      await disconnectTelegram(accessToken);

      updateConnection({
        status: "DISCONNECTED",
        connected: false,
        telegramUsername: null,
        connectedAt: null,
      });

      setDeepLink(null);
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        handleUnauthorized();

        return;
      }

      setErrorMessage(t("disconnectError"));
    } finally {
      setIsDisconnecting(false);
    }
  };

  if (isLoading) {
    return (
      <section>
        {showSectionTitle && <SectionTitle title={t("section")} />}

        <div className="flex min-h-28 items-center justify-center rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
          <LoaderCircle className="size-5 animate-spin text-[#8e8e93]" />
        </div>
      </section>
    );
  }

  const status = connection?.status ?? "DISCONNECTED";

  return (
    <section>
      {showSectionTitle && <SectionTitle title={t("section")} />}

      <div className="rounded-[22px] bg-white p-5 shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
        <div className="flex items-start gap-4">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-[15px] bg-[#229ed9]/10">
            <Send className="size-6 text-[#229ed9]" />
          </div>

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-[17px] font-semibold text-[#1c1c1e]">{t("title")}</h2>

              {status === "CONNECTED" && (
                <span className="inline-flex items-center gap-1 rounded-full bg-[#34c759]/10 px-2 py-1 text-[11px] font-semibold text-[#248a3d]">
                  <Check className="size-3" />

                  {t("connected")}
                </span>
              )}

              {status === "ALREADY_LINKED" && (
                <span className="inline-flex items-center gap-1 rounded-full bg-[#ff3b30]/10 px-2 py-1 text-[11px] font-semibold text-[#d70015]">
                  <CircleAlert className="size-3" />

                  {t("alreadyLinked")}
                </span>
              )}
            </div>

            <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{t("description")}</p>
          </div>
        </div>

        <div className="mt-5">
          {status === "CONNECTED" && (
            <div className="space-y-3">
              <div className="rounded-[14px] bg-[#34c759]/10 px-4 py-3 text-sm font-medium text-[#248a3d]">
                {connection?.telegramUsername
                  ? t("connectedAs", {
                      username: connection.telegramUsername,
                    })
                  : t("connected")}
              </div>

              <Button
                type="button"
                onClick={() => {
                  void handleTest();
                }}
                disabled={isTesting}
                className="h-11 w-full rounded-[13px] bg-[#229ed9] text-white shadow-none hover:bg-[#1d8fc5]"
              >
                <TestTube2 className="size-4" />

                {isTesting ? t("testing") : t("sendTest")}
              </Button>

              {allowDisconnect && (
                <Button
                  type="button"
                  variant="ghost"
                  disabled={isDisconnecting}
                  onClick={() => {
                    void handleDisconnect();
                  }}
                  className="h-11 w-full rounded-[13px] text-[#d70015] hover:bg-[#ff3b30]/10 hover:text-[#d70015]"
                >
                  <Unlink className="size-4" />

                  {isDisconnecting ? t("disconnecting") : t("disconnect")}
                </Button>
              )}
            </div>
          )}

          {status === "PENDING" && (
            <div className="space-y-4">
              <div className="rounded-[14px] bg-[#ff9500]/10 px-4 py-3">
                <p className="text-sm font-semibold text-[#c93400]">{t("pending")}</p>

                <p className="mt-1 text-xs leading-5 text-[#8e8e93]">{t("pendingHint")}</p>
              </div>

              {deepLink ? (
                <Button
                  type="button"
                  onClick={() => {
                    window.open(deepLink, "_blank", "noopener,noreferrer");
                  }}
                  className="h-11 w-full rounded-[13px] bg-[#229ed9] text-white hover:bg-[#1d8fc5]"
                >
                  <ExternalLink className="size-4" />

                  {t("openTelegram")}
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={() => {
                    void handleConnect();
                  }}
                  disabled={isConnecting}
                  className="h-11 w-full rounded-[13px] bg-[#229ed9] text-white hover:bg-[#1d8fc5]"
                >
                  {t("newLink")}
                </Button>
              )}
            </div>
          )}

          {status === "ALREADY_LINKED" && (
            <div className="space-y-4">
              <div className="rounded-[14px] bg-[#ff3b30]/10 px-4 py-3">
                <div className="flex items-start gap-3">
                  <CircleAlert className="mt-0.5 size-5 shrink-0 text-[#d70015]" />

                  <div>
                    <p className="text-sm font-semibold text-[#d70015]">{t("alreadyLinkedTitle")}</p>

                    <p className="mt-1 text-xs leading-5 text-[#636366]">{t("alreadyLinkedDescription")}</p>
                  </div>
                </div>
              </div>

              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  void handleConnect();
                }}
                disabled={isConnecting}
                className="h-11 w-full rounded-[13px] text-[#007aff] hover:bg-[#007aff]/8 hover:text-[#007aff]"
              >
                <RefreshCw className="size-4" />

                {isConnecting ? t("connecting") : t("newLink")}
              </Button>
            </div>
          )}

          {status === "DISCONNECTED" && (
            <Button
              type="button"
              onClick={() => {
                void handleConnect();
              }}
              disabled={isConnecting}
              className="h-11 w-full rounded-[13px] bg-[#229ed9] text-white shadow-none hover:bg-[#1d8fc5]"
            >
              <Send className="size-4" />

              {isConnecting ? t("connecting") : t("connect")}
            </Button>
          )}

          {successMessage && (
            <div className="mt-4 flex items-center gap-2 rounded-[14px] bg-[#34c759]/10 px-4 py-3 text-sm font-medium text-[#248a3d]">
              <Check className="size-4 shrink-0" />

              {successMessage}
            </div>
          )}

          {errorMessage && (
            <div role="alert" className="mt-4 rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
              {errorMessage}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

type SectionTitleProps = {
  title: string;
};

function SectionTitle({ title }: SectionTitleProps) {
  return <h2 className="mb-3 px-1 text-[13px] font-semibold uppercase tracking-[0.06em] text-[#8e8e93]">{title}</h2>;
}
