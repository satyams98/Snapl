import { apiFetch } from "./client";

export interface CustomDomain {
  id: number;
  domain: string;
  verified: boolean;
  txtRecordName: string;
  txtRecordValue: string;
  createdAt: string;
}

export function listDomains(): Promise<CustomDomain[]> {
  return apiFetch<CustomDomain[]>("/api/domains");
}

export function createDomain(domain: string): Promise<CustomDomain> {
  return apiFetch<CustomDomain>("/api/domains", {
    method: "POST",
    body: JSON.stringify({ domain }),
  });
}

export function verifyDomain(id: number): Promise<CustomDomain> {
  return apiFetch<CustomDomain>(`/api/domains/${id}/verify`, { method: "POST" });
}

export function deleteDomain(id: number): Promise<void> {
  return apiFetch(`/api/domains/${id}`, { method: "DELETE" });
}
