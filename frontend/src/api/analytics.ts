import { apiFetch } from "./client";

export interface DailyClickPoint {
  day: string;
  clicks: number;
}

export interface BreakdownItem {
  label: string;
  count: number;
}

export interface TopLinkItem {
  shortCode: string;
  longUrl: string;
  clicks: number;
}

export interface AnalyticsSummary {
  totalClicks: number;
  totalLinks: number;
  series: DailyClickPoint[];
  topLinks: TopLinkItem[];
}

export interface LinkAnalytics {
  shortCode: string;
  totalClicks: number;
  series: DailyClickPoint[];
  referrers: BreakdownItem[];
  devices: BreakdownItem[];
  browsers: BreakdownItem[];
  operatingSystems: BreakdownItem[];
}

export function getOrgSummary(days = 30): Promise<AnalyticsSummary> {
  return apiFetch<AnalyticsSummary>(`/api/analytics/summary?days=${days}`);
}

export function getLinkAnalytics(shortCode: string, days = 30): Promise<LinkAnalytics> {
  return apiFetch<LinkAnalytics>(`/api/urls/${shortCode}/analytics?days=${days}`);
}
