"use client";

import { type FormEvent, useState } from "react";

import { CalendarDays, ChevronRight, Pencil, Trash2, TriangleAlert } from "lucide-react";

import { useTranslations } from "next-intl";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { ApiClientError } from "@/lib/api/api-client";

import { deleteCycleRecord, updateCycleRecord } from "@/lib/api/cycle-api";

import type { CycleRecord } from "@/types/cycle";

type DialogMode = "edit" | "delete";

type ManageCycleDialogProps = {
  accessToken: string;
  record: CycleRecord;
  displayDate: string;
  secondaryText: string;
  onChanged: () => Promise<void>;
  onUnauthorized: () => void;
};

function getTodayInputValue(): string {
  const today = new Date();

  const year = today.getFullYear();

  const month = String(today.getMonth() + 1).padStart(2, "0");

  const day = String(today.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

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

export function ManageCycleDialog({
  accessToken,
  record,
  displayDate,
  secondaryText,
  onChanged,
  onUnauthorized,
}: ManageCycleDialogProps) {
  const t = useTranslations("Dashboard");

  const [open, setOpen] = useState(false);

  const [mode, setMode] = useState<DialogMode>("edit");

  const [startDate, setStartDate] = useState(record.startDate);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const today = getTodayInputValue();

  const resetDialog = () => {
    setMode("edit");
    setStartDate(record.startDate);
    setErrorMessage(null);
    setIsSubmitting(false);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (nextOpen) {
      resetDialog();
    }

    setOpen(nextOpen);
  };

  const closeDialog = () => {
    setOpen(false);
    resetDialog();
  };

  const handleUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!startDate) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await updateCycleRecord(accessToken, record.id, {
        startDate,
      });

      await onChanged();

      closeDialog();
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        onUnauthorized();
        return;
      }

      setErrorMessage(getErrorMessage(error, t("updateError")));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await deleteCycleRecord(accessToken, record.id);

      await onChanged();

      closeDialog();
    } catch (error) {
      if (error instanceof ApiClientError && error.status === 401) {
        onUnauthorized();
        return;
      }

      setErrorMessage(getErrorMessage(error, t("deleteError")));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <button
          type="button"
          className="flex min-h-17.5 w-full items-center gap-4 px-4 py-3 text-left transition hover:bg-black/2.5 active:bg-black/5"
          aria-label={`${t("editPeriod")}: ${displayDate}`}
        >
          <div className="flex size-10 shrink-0 items-center justify-center rounded-[13px] bg-[#ff2d55]/10">
            <CalendarDays className="size-5 text-[#ff2d55]" />
          </div>

          <div className="min-w-0 flex-1">
            <p className="truncate text-[16px] font-semibold text-[#1c1c1e]">{displayDate}</p>

            <p className="mt-0.5 text-[13px] text-[#8e8e93]">{secondaryText}</p>
          </div>

          <ChevronRight className="size-4 shrink-0 text-[#c7c7cc]" />
        </button>
      </DialogTrigger>

      <DialogContent className="w-[calc(100%-2rem)] max-w-sm overflow-hidden rounded-[26px] border-0 bg-white p-0 shadow-[0_20px_70px_rgba(0,0,0,0.18)]">
        {mode === "edit" ? (
          <form onSubmit={handleUpdate}>
            <DialogHeader className="px-6 pb-3 pt-7 text-left">
              <div className="mb-3 flex size-12 items-center justify-center rounded-[15px] bg-[#007aff]/10">
                <Pencil className="size-6 text-[#007aff]" />
              </div>

              <DialogTitle className="text-[22px] font-bold tracking-[-0.03em] text-[#1c1c1e]">
                {t("editPeriodTitle")}
              </DialogTitle>

              <DialogDescription className="text-[14px] leading-5 text-[#636366]">
                {t("editPeriodDescription")}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 px-6 py-3">
              <div className="space-y-2">
                <Label htmlFor={`cycle-date-${record.id}`} className="text-[13px] font-semibold text-[#3a3a3c]">
                  {t("dateLabel")}
                </Label>

                <Input
                  id={`cycle-date-${record.id}`}
                  name="startDate"
                  type="date"
                  max={today}
                  value={startDate}
                  onChange={(event) => setStartDate(event.target.value)}
                  className="h-12 rounded-[14px] border-0 bg-[#f2f2f7] px-4 text-[16px] shadow-none focus-visible:ring-2 focus-visible:ring-[#007aff]/30"
                  required
                />
              </div>

              {errorMessage && (
                <div role="alert" className="rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
                  {errorMessage}
                </div>
              )}
            </div>

            <div className="space-y-3 px-6 pb-6 pt-3">
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="ghost"
                  disabled={isSubmitting}
                  onClick={closeDialog}
                  className="h-11 flex-1 rounded-[13px] bg-[#f2f2f7] text-[15px] font-semibold text-[#3a3a3c] hover:bg-[#e5e5ea]"
                >
                  {t("cancel")}
                </Button>

                <Button
                  type="submit"
                  disabled={isSubmitting || !startDate || startDate === record.startDate}
                  className="h-11 flex-1 rounded-[13px] bg-[#007aff] text-[15px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
                >
                  {isSubmitting ? t("updating") : t("update")}
                </Button>
              </div>

              <Button
                type="button"
                variant="ghost"
                disabled={isSubmitting}
                onClick={() => {
                  setErrorMessage(null);
                  setMode("delete");
                }}
                className="h-11 w-full rounded-[13px] text-[15px] font-semibold text-[#d70015] hover:bg-[#ff3b30]/10 hover:text-[#d70015]"
              >
                <Trash2 className="size-4" />

                {t("deletePeriod")}
              </Button>
            </div>
          </form>
        ) : (
          <div>
            <DialogHeader className="px-6 pb-3 pt-7 text-left">
              <div className="mb-3 flex size-12 items-center justify-center rounded-[15px] bg-[#ff3b30]/10">
                <TriangleAlert className="size-6 text-[#d70015]" />
              </div>

              <DialogTitle className="text-[22px] font-bold tracking-[-0.03em] text-[#1c1c1e]">
                {t("deleteConfirmTitle")}
              </DialogTitle>

              <DialogDescription className="text-[14px] leading-5 text-[#636366]">
                {t("deleteConfirmDescription")}
              </DialogDescription>
            </DialogHeader>

            <div className="px-6 py-3">
              <div className="rounded-[14px] bg-[#f2f2f7] px-4 py-3">
                <p className="text-[15px] font-semibold text-[#1c1c1e]">{displayDate}</p>

                <p className="mt-0.5 text-[13px] text-[#8e8e93]">{secondaryText}</p>
              </div>

              {errorMessage && (
                <div role="alert" className="mt-4 rounded-[14px] bg-[#ff3b30]/10 px-4 py-3 text-sm text-[#d70015]">
                  {errorMessage}
                </div>
              )}
            </div>

            <div className="flex gap-2 px-6 pb-6 pt-3">
              <Button
                type="button"
                variant="ghost"
                disabled={isSubmitting}
                onClick={() => {
                  setErrorMessage(null);
                  setMode("edit");
                }}
                className="h-11 flex-1 rounded-[13px] bg-[#f2f2f7] text-[15px] font-semibold text-[#3a3a3c] hover:bg-[#e5e5ea]"
              >
                {t("keepRecord")}
              </Button>

              <Button
                type="button"
                disabled={isSubmitting}
                onClick={() => {
                  void handleDelete();
                }}
                className="h-11 flex-1 rounded-[13px] bg-[#ff3b30] text-[15px] font-semibold text-white shadow-none hover:bg-[#e6352b]"
              >
                {isSubmitting ? t("deleting") : t("delete")}
              </Button>
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
