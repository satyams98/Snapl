import { apiFetch } from "./client";

export interface ApiKeySummary {
  id: number;
  name: string;
  keyPrefix: string;
  scopes: string[];
  revoked: boolean;
  createdAt: string;
  lastUsedAt: string | null;
}

export interface ApiKeyCreated {
  id: number;
  name: string;
  apiKey: string;
  scopes: string[];
  createdAt: string;
}

export function listApiKeys(): Promise<ApiKeySummary[]> {
  return apiFetch<ApiKeySummary[]>("/api/api-keys");
}

export function createApiKey(name: string, scopes: string[]): Promise<ApiKeyCreated> {
  return apiFetch<ApiKeyCreated>("/api/api-keys", {
    method: "POST",
    body: JSON.stringify({ name, scopes }),
  });
}

export function revokeApiKey(id: number): Promise<void> {
  return apiFetch(`/api/api-keys/${id}`, { method: "DELETE" });
}
