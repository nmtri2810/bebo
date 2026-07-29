import { apiRequest } from "@/lib/api/api-client";

import type { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

export function login(request: LoginRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function register(request: RegisterRequest): Promise<AuthResponse> {
  return apiRequest<AuthResponse>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
