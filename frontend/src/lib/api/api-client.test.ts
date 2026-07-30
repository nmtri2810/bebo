import { describe, expect, it, vi } from "vitest";

import { ApiClientError, apiRequest } from "@/lib/api/api-client";

describe("apiRequest", () => {
  it("returns undefined for 204 responses", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));

    vi.stubGlobal("fetch", fetchMock);

    await expect(
      apiRequest<void>("/api/notification-channels/telegram", {
        method: "DELETE",
        token: "access-token",
      }),
    ).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/notification-channels/telegram",
      expect.objectContaining({
        cache: "no-store",
        method: "DELETE",
        headers: expect.any(Headers),
      }),
    );

    const headers = fetchMock.mock.calls[0]?.[1]?.headers as Headers;

    expect(headers.get("Authorization")).toBe("Bearer access-token");
  });

  it("throws ApiClientError with backend field errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            code: "VALIDATION_ERROR",
            message: "Invalid request",
            fieldErrors: {
              timezone: "Use an IANA time zone.",
            },
          },
          { status: 400 },
        ),
      ),
    );

    await expect(apiRequest("/api/settings", { method: "PUT" })).rejects.toMatchObject<ApiClientError>({
      status: 400,
      code: "VALIDATION_ERROR",
      message: "Invalid request",
      fieldErrors: {
        timezone: "Use an IANA time zone.",
      },
    });
  });
});
