"use client";

import { Toaster as SonnerToaster } from "sonner";

export function Toaster() {
  return (
    <SonnerToaster
      position="bottom-right"
      richColors
      closeButton
      offset={{
        right: 24,
        bottom: 24,
      }}
      mobileOffset={{
        right: 16,
        bottom: "calc(5.75rem + env(safe-area-inset-bottom))",
        left: 16,
      }}
      toastOptions={{
        classNames: {
          toast: "rounded-[14px] border border-black/10 shadow-[0_12px_34px_rgba(0,0,0,0.14)]",
          title: "text-[14px] font-semibold",
          description: "text-[13px]",
          actionButton: "cursor-pointer",
          cancelButton: "cursor-pointer",
          closeButton: "cursor-pointer",
        },
      }}
    />
  );
}
