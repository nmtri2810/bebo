import { expect, test, type Page, type Route } from "@playwright/test";

const user = {
  id: "00000000-0000-4000-8000-000000000001",
  email: "e2e@example.com",
  timezone: "Asia/Ho_Chi_Minh",
  onboardingStep: "COMPLETED",
  onboardingCompletedAt: "2026-07-29T08:00:00Z",
};

const settings = {
  defaultCycleLength: 28,
  reminderDaysBefore: 2,
  notificationTime: "08:30",
  timezone: "Asia/Ho_Chi_Minh",
};

const connectedTelegram = {
  status: "CONNECTED",
  connected: true,
  telegramUsername: "bebo_user",
  connectedAt: "2026-07-29T08:00:00Z",
};

const disconnectedTelegram = {
  status: "DISCONNECTED",
  connected: false,
  telegramUsername: null,
  connectedAt: null,
};

async function seedAuth(page: Page) {
  await page.addInitScript((authUser) => {
    window.localStorage.setItem(
      "bebo-auth",
      JSON.stringify({
        state: {
          accessToken: "e2e-token",
          user: authUser,
        },
        version: 0,
      }),
    );

    document.cookie = "locale=en; path=/";
  }, user);
}

async function mockApi(page: Page) {
  let telegramConnection = connectedTelegram;

  await page.route("**/api/**", async (route: Route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    const path = url.pathname;

    if (method === "GET" && path === "/api/users/me") {
      await route.fulfill({ json: user });
      return;
    }

    if (method === "GET" && path === "/api/settings") {
      await route.fulfill({ json: settings });
      return;
    }

    if (method === "PUT" && path === "/api/settings") {
      const requestBody = request.postDataJSON();

      await route.fulfill({
        json: {
          ...settings,
          ...requestBody,
        },
      });
      return;
    }

    if (method === "GET" && path === "/api/notification-channels/telegram") {
      await route.fulfill({ json: telegramConnection });
      return;
    }

    if (method === "POST" && path === "/api/notification-channels/telegram/test") {
      await route.fulfill({
        json: {
          sent: true,
          sentAt: "2026-07-29T08:01:00Z",
        },
      });
      return;
    }

    if (method === "DELETE" && path === "/api/notification-channels/telegram") {
      telegramConnection = disconnectedTelegram;

      await route.fulfill({ status: 204 });
      return;
    }

    if (method === "GET" && path === "/api/notification-channels/discord") {
      await route.fulfill({
        json: {
          status: "DISCONNECTED",
          connected: false,
          discordUsername: null,
          connectedAt: null,
        },
      });
      return;
    }

    await route.fulfill({
      status: 404,
      json: {
        message: `Unhandled mocked API route: ${method} ${path}`,
      },
    });
  });
}

async function expectToastAtBottomRight(page: Page, text: string) {
  const toast = page.getByText(text).locator("xpath=ancestor::*[@data-sonner-toast][1]");

  await expect(toast).toBeVisible();

  const boundingBox = await toast.boundingBox();
  const viewportSize = page.viewportSize();

  expect(boundingBox).not.toBeNull();
  expect(viewportSize).not.toBeNull();
  expect(boundingBox!.x + boundingBox!.width / 2).toBeGreaterThan(viewportSize!.width / 2);
  expect(boundingBox!.y + boundingBox!.height / 2).toBeGreaterThan(viewportSize!.height / 2);
}

test.beforeEach(async ({ page }) => {
  await mockApi(page);
  await seedAuth(page);
});

test("settings actions show bottom-right sonner toasts without inline success", async ({ page }) => {
  await page.goto("/settings");

  await expect(page.getByRole("heading", { name: "Settings" })).toBeVisible();
  await expect(page.getByText("Connected as @bebo_user")).toBeVisible();

  await page.getByRole("button", { name: "Send test notification" }).click();

  await expectToastAtBottomRight(page, "Test notification sent to Telegram.");

  await page.getByRole("button", { name: "Disconnect" }).click();

  await expectToastAtBottomRight(page, "Telegram disconnected.");
  await expect(page.getByRole("button", { name: "Connect Telegram" })).toBeVisible();
  await expect(page.getByText("We couldn't disconnect Telegram.")).toBeHidden();

  await page.getByRole("button", { name: "Save changes" }).click();

  await expectToastAtBottomRight(page, "Settings saved");
  await expect(page.getByRole("status").filter({ hasText: "Settings saved" })).toHaveCount(0);
});
