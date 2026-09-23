import { apiFetch } from "./client";

export interface Plan {
  code: string;
  name: string;
  priceCents: number;
  maxLinks: number;
  maxCustomDomains: number;
  maxClicksPerMonth: number;
  apiAccessAllowed: boolean;
}

export interface UsageSummary {
  activeLinks: number;
  maxLinks: number;
  customDomains: number;
  maxCustomDomains: number;
  apiAccessAllowed: boolean;
  planCode: string;
  planName: string;
  subscriptionStatus: string;
}

export function listPlans(): Promise<Plan[]> {
  return apiFetch<Plan[]>("/api/billing/plans");
}

export function getUsageSummary(): Promise<UsageSummary> {
  return apiFetch<UsageSummary>("/api/billing/summary");
}

export function createCheckoutSession(planCode: string): Promise<{ url: string }> {
  return apiFetch<{ url: string }>("/api/billing/checkout-session", {
    method: "POST",
    body: JSON.stringify({ planCode }),
  });
}
