"use client";

import { type FormEvent, useState } from "react";

import { CalendarDays, Plus } from "lucide-react";

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

import { createCycleRecord } from "@/lib/api/cycle-api";

type AddCycleDialogProps = {
  accessToken: string;
  onCreated: () => Promise<void>;
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

export function AddCycleDialog({ accessToken, onCreated }: AddCycleDialogProps) {
  const t = useTranslations("Dashboard");

  const [open, setOpen] = useState(false);

  const [startDate, setStartDate] = useState(() => getTodayInputValue());

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const today = getTodayInputValue();

  const resetForm = () => {
    setStartDate(getTodayInputValue());
    setErrorMessage(null);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    setOpen(nextOpen);

    /*
     * Reset trực tiếp trong event handler,
     * không cần dùng useEffect.
     */
    if (!nextOpen) {
      resetForm();
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!startDate) {
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await createCycleRecord(accessToken, {
        startDate,
      });

      await onCreated();

      handleOpenChange(false);
    } catch (error) {
      setErrorMessage(getErrorMessage(error, t("saveError")));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button className="h-10 rounded-full bg-[#007aff] px-4 text-sm font-semibold text-white shadow-none hover:bg-[#006ee6]">
          <Plus className="size-4" />

          {t("addPeriod")}
        </Button>
      </DialogTrigger>

      <DialogContent className="w-[calc(100%-2rem)] max-w-sm overflow-hidden rounded-[26px] border-0 bg-white p-0 shadow-[0_20px_70px_rgba(0,0,0,0.18)]">
        <form onSubmit={handleSubmit}>
          <DialogHeader className="px-6 pb-3 pt-7 text-left">
            <div className="mb-3 flex size-12 items-center justify-center rounded-[15px] bg-[#ff2d55]/10">
              <CalendarDays className="size-6 text-[#ff2d55]" />
            </div>

            <DialogTitle className="text-[22px] font-bold tracking-[-0.03em] text-[#1c1c1e]">
              {t("addPeriodTitle")}
            </DialogTitle>

            <DialogDescription className="text-[14px] leading-5 text-[#636366]">
              {t("addPeriodDescription")}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 px-6 py-3">
            <div className="space-y-2">
              <Label htmlFor="dashboard-start-date" className="text-[13px] font-semibold text-[#3a3a3c]">
                {t("dateLabel")}
              </Label>

              <Input
                id="dashboard-start-date"
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

          {/*
           * Không dùng DialogFooter ở đây.
           * Plain div giúp tránh style mặc định
           * tạo vùng footer thừa.
           */}
          <div className="flex gap-2 px-6 pb-6 pt-3">
            <Button
              type="button"
              variant="ghost"
              disabled={isSubmitting}
              onClick={() => handleOpenChange(false)}
              className="h-11 flex-1 rounded-[13px] bg-[#f2f2f7] text-[15px] font-semibold text-[#3a3a3c] hover:bg-[#e5e5ea]"
            >
              {t("cancel")}
            </Button>

            <Button
              type="submit"
              disabled={isSubmitting || !startDate}
              className="h-11 flex-1 rounded-[13px] bg-[#007aff] text-[15px] font-semibold text-white shadow-none hover:bg-[#006ee6]"
            >
              {isSubmitting ? t("saving") : t("save")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
