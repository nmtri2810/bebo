import { apiRequest } from "@/lib/api/api-client";

import type { CreateCycleRecordRequest, CyclePrediction, CycleRecord, UpdateCycleRecordRequest } from "@/types/cycle";

export function getCycleHistory(accessToken: string): Promise<CycleRecord[]> {
  return apiRequest<CycleRecord[]>("/api/cycles", {
    method: "GET",
    token: accessToken,
  });
}

export function createCycleRecord(accessToken: string, request: CreateCycleRecordRequest): Promise<CycleRecord> {
  return apiRequest<CycleRecord>("/api/cycles", {
    method: "POST",
    token: accessToken,
    body: JSON.stringify(request),
  });
}

export function updateCycleRecord(
  accessToken: string,
  recordId: string,
  request: UpdateCycleRecordRequest,
): Promise<CycleRecord> {
  return apiRequest<CycleRecord>(`/api/cycles/${recordId}`, {
    method: "PATCH",
    token: accessToken,
    body: JSON.stringify(request),
  });
}

export function deleteCycleRecord(accessToken: string, recordId: string): Promise<void> {
  return apiRequest<void>(`/api/cycles/${recordId}`, {
    method: "DELETE",
    token: accessToken,
  });
}

export function getCyclePrediction(accessToken: string): Promise<CyclePrediction> {
  return apiRequest<CyclePrediction>("/api/cycles/prediction", {
    method: "GET",
    token: accessToken,
  });
}
