export type AuthUser = {
  id: string;
  email: string;
  timezone: string;
  status?: string;
  createdAt?: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  userId: string;
  email: string;
  timezone: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  timezone: string;
};

export type ApiErrorResponse = {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
  fieldErrors?: Record<string, string>;
};
