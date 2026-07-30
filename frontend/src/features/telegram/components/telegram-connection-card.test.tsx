import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NextIntlClientProvider } from "next-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";

import enMessages from "../../../../messages/en.json";

import { TelegramConnectionCard } from "@/features/telegram/components/telegram-connection-card";
import {
  beginTelegramConnection,
  disconnectTelegram,
  getTelegramConnection,
  sendTelegramTest,
} from "@/lib/api/telegram-api";

const routerReplace = vi.hoisted(() => vi.fn());
const toastMock = vi.hoisted(() => ({
  error: vi.fn(),
  info: vi.fn(),
  success: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: routerReplace,
  }),
}));

vi.mock("sonner", () => ({
  toast: toastMock,
}));

vi.mock("@/lib/api/telegram-api", () => ({
  beginTelegramConnection: vi.fn(),
  disconnectTelegram: vi.fn(),
  getTelegramConnection: vi.fn(),
  sendTelegramTest: vi.fn(),
}));

function renderTelegramConnectionCard() {
  return render(
    <NextIntlClientProvider locale="en" messages={enMessages}>
      <TelegramConnectionCard accessToken="access-token" showSectionTitle={false} />
    </NextIntlClientProvider>,
  );
}

describe("TelegramConnectionCard", () => {
  beforeEach(() => {
    vi.mocked(getTelegramConnection).mockResolvedValue({
      status: "CONNECTED",
      connected: true,
      telegramUsername: "bebo_user",
      connectedAt: "2026-07-29T08:00:00Z",
    });

    vi.mocked(beginTelegramConnection).mockResolvedValue({
      status: "PENDING",
      deepLink: "https://t.me/bebo_test_bot?start=e2e-token",
      expiresAt: "2026-07-29T08:05:00Z",
    });

    vi.mocked(disconnectTelegram).mockResolvedValue(undefined);
    vi.mocked(sendTelegramTest).mockResolvedValue({
      sent: true,
      sentAt: "2026-07-29T08:01:00Z",
    });
  });

  it("shows a success toast after disconnecting", async () => {
    const user = userEvent.setup();

    renderTelegramConnectionCard();

    expect(await screen.findByText("Connected as @bebo_user")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /disconnect/i }));

    await waitFor(() => {
      expect(disconnectTelegram).toHaveBeenCalledWith("access-token");
    });

    await waitFor(() => {
      expect(toastMock.success).toHaveBeenCalledWith("Telegram disconnected.", {
        id: "telegram-disconnected",
      });
    });
  });

  it("shows a success toast after sending a test notification", async () => {
    const user = userEvent.setup();

    renderTelegramConnectionCard();

    await screen.findByText("Connected as @bebo_user");
    await user.click(screen.getByRole("button", { name: /send test notification/i }));

    await waitFor(() => {
      expect(sendTelegramTest).toHaveBeenCalledWith("access-token");
    });

    expect(toastMock.success).toHaveBeenCalledWith("Test notification sent to Telegram.", {
      id: "telegram-test-sent",
    });
  });
});
