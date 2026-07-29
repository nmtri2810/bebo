"use client";

import { useEffect, useState } from "react";

import {
  BellRing,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  CircleX,
  Clock3,
  LoaderCircle,
  RefreshCw,
  Send,
} from "lucide-react";

import Link from "next/link";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";

import { LanguageSwitcher } from "@/components/language-switcher";
import { Button } from "@/components/ui/button";

import { ApiClientError } from "@/lib/api/api-client";

import { getNotificationHistory } from "@/lib/api/notification-history-api";

import { useAuthStore } from "@/stores/auth-store";

import type { NotificationHistoryItem, NotificationHistoryPage } from "@/types/notification-history";

const PAGE_SIZE = 20;

type Copy = {
  back: string;
  title: string;
  description: string;
  loading: string;
  loadError: string;
  retry: string;
  emptyTitle: string;
  emptyDescription: string;
  estimatedPeriod: string;
  sent: string;
  failed: string;
  retryScheduled: string;
  telegram: string;
  sentAt: string;
  scheduledFor: string;
  nextRetry: string;
  attempts: string;
  attempt: string;
  failureDetails: string;
  previous: string;
  next: string;
  page: string;
};

const copyByLocale: Record<"en" | "vi", Copy> = {
  en: {
    back: "Settings",
    title: "Notification history",
    description: "Review cycle reminders sent through Telegram.",
    loading: "Loading notification history...",
    loadError: "We couldn't load notification history.",
    retry: "Try again",
    emptyTitle: "No notifications yet",
    emptyDescription: "Sent and failed cycle reminders will appear here.",
    estimatedPeriod: "Estimated period",
    sent: "Sent",
    failed: "Failed",
    retryScheduled: "Retry scheduled",
    telegram: "Telegram",
    sentAt: "Sent",
    scheduledFor: "Scheduled",
    nextRetry: "Next retry",
    attempts: "attempts",
    attempt: "attempt",
    failureDetails: "Failure details",
    previous: "Previous",
    next: "Next",
    page: "Page",
  },

  vi: {
    back: "Cài đặt",
    title: "Lịch sử thông báo",
    description: "Xem lại các lời nhắc chu kỳ được gửi qua Telegram.",
    loading: "Đang tải lịch sử thông báo...",
    loadError: "Không thể tải lịch sử thông báo.",
    retry: "Thử lại",
    emptyTitle: "Chưa có thông báo",
    emptyDescription: "Các lời nhắc đã gửi hoặc gửi thất bại sẽ xuất hiện tại đây.",
    estimatedPeriod: "Kỳ kinh dự kiến",
    sent: "Đã gửi",
    failed: "Thất bại",
    retryScheduled: "Đã lên lịch thử lại",
    telegram: "Telegram",
    sentAt: "Đã gửi",
    scheduledFor: "Dự kiến gửi",
    nextRetry: "Thử lại lúc",
    attempts: "lần thử",
    attempt: "lần thử",
    failureDetails: "Chi tiết lỗi",
    previous: "Trước",
    next: "Sau",
    page: "Trang",
  },
};

function resolveCopy(locale: string): Copy {
  return locale.startsWith("vi") ? copyByLocale.vi : copyByLocale.en;
}

function parseLocalDate(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);

  return new Date(year, month - 1, day);
}

function formatPredictedDate(value: string, locale: string): string {
  return new Intl.DateTimeFormat(locale.startsWith("vi") ? "vi-VN" : "en-US", {
    dateStyle: "long",
  }).format(parseLocalDate(value));
}

function formatInstant(value: string, locale: string, timezone: string): string {
  return new Intl.DateTimeFormat(locale.startsWith("vi") ? "vi-VN" : "en-US", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: timezone,
  }).format(new Date(value));
}

async function fetchHistory(accessToken: string, page: number): Promise<NotificationHistoryPage> {
  return getNotificationHistory(accessToken, {
    page,
    size: PAGE_SIZE,
  });
}

export default function NotificationsPage() {
  const router = useRouter();
  const locale = useLocale();
  const copy = resolveCopy(locale);

  const accessToken = useAuthStore((state) => state.accessToken);

  const user = useAuthStore((state) => state.user);

  const hasHydrated = useAuthStore((state) => state.hasHydrated);

  const clearSession = useAuthStore((state) => state.clearSession);

  const [page, setPage] = useState(0);

  const [historyPage, setHistoryPage] = useState<NotificationHistoryPage | null>(null);

  const [isLoading, setIsLoading] = useState(true);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!hasHydrated) {
      return;
    }

    if (!accessToken) {
      router.replace("/");
      return;
    }

    let cancelled = false;

    fetchHistory(accessToken, page)
      .then((result) => {
        if (cancelled) {
          return;
        }

        setHistoryPage(result);
        setErrorMessage(null);
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }

        if (error instanceof ApiClientError && error.status === 401) {
          clearSession();
          router.replace("/");
          return;
        }

        setErrorMessage(copy.loadError);
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, clearSession, copy.loadError, hasHydrated, page, router]);

  const loadPage = (nextPage: number) => {
    if (nextPage < 0 || nextPage === page) {
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);
    setPage(nextPage);
  };

  const reloadCurrentPage = () => {
    if (!accessToken) {
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    fetchHistory(accessToken, page)
      .then((result) => {
        setHistoryPage(result);
      })
      .catch((error: unknown) => {
        if (error instanceof ApiClientError && error.status === 401) {
          clearSession();
          router.replace("/");
          return;
        }

        setErrorMessage(copy.loadError);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  if (!hasHydrated) {
    return <LoadingPage message={copy.loading} />;
  }

  if (!accessToken) {
    return null;
  }

  const timezone = user?.timezone ?? "UTC";

  return (
    <main className="min-h-dvh bg-[#f2f2f7] px-4 py-6 sm:py-8">
      <div className="mx-auto w-full max-w-140">
        <header className="mb-8 flex items-center justify-between">
          <Link
            href="/settings"
            className="inline-flex h-10 items-center gap-1 rounded-full px-2 text-sm font-semibold text-[#007aff] transition hover:bg-black/4"
          >
            <ChevronLeft className="size-5" />

            {copy.back}
          </Link>

          <LanguageSwitcher />
        </header>

        <div className="mb-7 flex items-start gap-3">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-[15px] bg-linear-to-br from-[#5856d6] to-[#af52de] shadow-[0_6px_16px_rgba(88,86,214,0.2)]">
            <BellRing className="size-6 text-white" />
          </div>

          <div>
            <h1 className="text-[30px] font-bold leading-tight tracking-[-0.04em] text-[#1c1c1e]">{copy.title}</h1>

            <p className="mt-1 text-sm leading-5 text-[#8e8e93]">{copy.description}</p>
          </div>
        </div>

        {errorMessage && (
          <div className="mb-5 rounded-[18px] bg-[#ff3b30]/10 p-4">
            <p className="text-sm text-[#d70015]">{errorMessage}</p>

            <Button
              type="button"
              variant="ghost"
              onClick={reloadCurrentPage}
              className="mt-2 h-9 rounded-[11px] px-3 text-[#d70015] hover:bg-[#ff3b30]/10 hover:text-[#d70015]"
            >
              <RefreshCw className="size-4" />

              {copy.retry}
            </Button>
          </div>
        )}

        {isLoading ? (
          <div className="flex min-h-52 flex-col items-center justify-center rounded-[24px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
            <LoaderCircle className="size-6 animate-spin text-[#8e8e93]" />

            <p className="mt-3 text-sm text-[#8e8e93]">{copy.loading}</p>
          </div>
        ) : historyPage && historyPage.items.length > 0 ? (
          <div className="space-y-4">
            {historyPage.items.map((item) => (
              <NotificationHistoryCard key={item.id} item={item} copy={copy} locale={locale} timezone={timezone} />
            ))}

            {historyPage.totalPages > 1 && (
              <div className="flex items-center justify-between pt-2">
                <Button
                  type="button"
                  variant="ghost"
                  disabled={historyPage.first}
                  onClick={() => loadPage(historyPage.page - 1)}
                  className="h-10 rounded-full bg-white px-4 shadow-[0_3px_12px_rgba(0,0,0,0.05)]"
                >
                  <ChevronLeft className="size-4" />

                  {copy.previous}
                </Button>

                <p className="text-sm text-[#8e8e93]">
                  {copy.page} {historyPage.page + 1}
                  {" / "}
                  {historyPage.totalPages}
                </p>

                <Button
                  type="button"
                  variant="ghost"
                  disabled={historyPage.last}
                  onClick={() => loadPage(historyPage.page + 1)}
                  className="h-10 rounded-full bg-white px-4 shadow-[0_3px_12px_rgba(0,0,0,0.05)]"
                >
                  {copy.next}

                  <ChevronRight className="size-4" />
                </Button>
              </div>
            )}
          </div>
        ) : (
          <div className="flex min-h-56 flex-col items-center justify-center rounded-[24px] bg-white px-6 text-center shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
            <div className="flex size-14 items-center justify-center rounded-full bg-[#5856d6]/10">
              <BellRing className="size-7 text-[#5856d6]" />
            </div>

            <h2 className="mt-4 text-[18px] font-semibold text-[#1c1c1e]">{copy.emptyTitle}</h2>

            <p className="mt-1 max-w-85 text-sm leading-5 text-[#8e8e93]">{copy.emptyDescription}</p>
          </div>
        )}
      </div>
    </main>
  );
}

type NotificationHistoryCardProps = {
  item: NotificationHistoryItem;
  copy: Copy;
  locale: string;
  timezone: string;
};

function NotificationHistoryCard({ item, copy, locale, timezone }: NotificationHistoryCardProps) {
  const isSent = item.status === "SENT";

  const hasScheduledRetry = item.status === "FAILED" && item.nextRetryAt !== null;

  return (
    <article className="overflow-hidden rounded-[22px] bg-white shadow-[0_5px_20px_rgba(0,0,0,0.05)]">
      <div className="flex items-start gap-4 p-5">
        <div
          className={
            isSent
              ? "flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#34c759]/10"
              : "flex size-11 shrink-0 items-center justify-center rounded-[14px] bg-[#ff3b30]/10"
          }
        >
          {isSent ? <CheckCircle2 className="size-6 text-[#248a3d]" /> : <CircleX className="size-6 text-[#d70015]" />}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-[16px] font-semibold text-[#1c1c1e]">{copy.estimatedPeriod}</h2>

            <span
              className={
                isSent
                  ? "rounded-full bg-[#34c759]/10 px-2 py-1 text-[11px] font-semibold text-[#248a3d]"
                  : "rounded-full bg-[#ff3b30]/10 px-2 py-1 text-[11px] font-semibold text-[#d70015]"
              }
            >
              {isSent ? copy.sent : copy.failed}
            </span>
          </div>

          <p className="mt-1 text-[17px] font-semibold text-[#1c1c1e]">
            {formatPredictedDate(item.predictedPeriodDate, locale)}
          </p>

          <div className="mt-2 flex items-center gap-1.5 text-[13px] text-[#8e8e93]">
            <Send className="size-3.5 text-[#229ed9]" />

            {copy.telegram}
          </div>
        </div>
      </div>

      <div className="border-t border-black/6 px-5 py-4">
        <div className="space-y-3">
          <HistoryDetail
            icon={<Clock3 className="size-4 text-[#007aff]" />}
            label={isSent ? copy.sentAt : copy.scheduledFor}
            value={formatInstant(isSent && item.sentAt ? item.sentAt : item.scheduledFor, locale, timezone)}
          />

          <HistoryDetail
            icon={
              isSent ? (
                <CheckCircle2 className="size-4 text-[#248a3d]" />
              ) : (
                <RefreshCw className="size-4 text-[#ff9500]" />
              )
            }
            label={item.attemptCount === 1 ? copy.attempt : copy.attempts}
            value={String(item.attemptCount)}
          />

          {hasScheduledRetry && item.nextRetryAt && (
            <HistoryDetail
              icon={<RefreshCw className="size-4 text-[#ff9500]" />}
              label={copy.nextRetry}
              value={formatInstant(item.nextRetryAt, locale, timezone)}
            />
          )}
        </div>

        {!isSent && item.failureMessage && (
          <details className="mt-4 rounded-[14px] bg-[#ff3b30]/6 px-4 py-3">
            <summary className="cursor-pointer text-sm font-semibold text-[#d70015]">
              {hasScheduledRetry ? copy.retryScheduled : copy.failureDetails}
            </summary>

            <p className="mt-2 text-sm leading-5 text-[#8e8e93]">{item.failureMessage}</p>
          </details>
        )}
      </div>
    </article>
  );
}

type HistoryDetailProps = {
  icon: React.ReactNode;
  label: string;
  value: string;
};

function HistoryDetail({ icon, label, value }: HistoryDetailProps) {
  return (
    <div className="flex items-center gap-2 text-sm">
      <div className="flex size-7 shrink-0 items-center justify-center rounded-[9px] bg-[#f2f2f7]">{icon}</div>

      <span className="flex-1 text-[#636366]">{label}</span>

      <span className="max-w-[55%] text-right font-medium text-[#1c1c1e]">{value}</span>
    </div>
  );
}

type LoadingPageProps = {
  message: string;
};

function LoadingPage({ message }: LoadingPageProps) {
  return (
    <main className="grid min-h-dvh place-items-center bg-[#f2f2f7]">
      <div className="flex flex-col items-center gap-3">
        <LoaderCircle className="size-6 animate-spin text-[#8e8e93]" />

        <p className="text-sm text-[#8e8e93]">{message}</p>
      </div>
    </main>
  );
}
