import type { ApiErrorResponse } from "@/types/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type ApiRequestOptions = RequestInit & {
  token?: string;
};

export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);

    this.name = "ApiClientError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { token, headers: initialHeaders, ...requestOptions } = options;

  const headers = new Headers(initialHeaders);

  if (requestOptions.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...requestOptions,
    headers,
    cache: "no-store",
  });

  if (!response.ok) {
    let errorBody: ApiErrorResponse | null = null;

    try {
      errorBody = (await response.json()) as ApiErrorResponse;
    } catch {
      // Backend không trả JSON hoặc response body rỗng.
    }

    throw new ApiClientError(
      response.status,
      errorBody?.code ?? "REQUEST_FAILED",
      errorBody?.message ?? `Request failed with status ${response.status}`,
      errorBody?.fieldErrors ?? {},
    );
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
