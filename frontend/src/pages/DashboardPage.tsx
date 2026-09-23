import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ClicksLineChart } from "@/components/analytics/ClicksLineChart";
import { getOrgSummary } from "@/api/analytics";
import { getUsageSummary } from "@/api/billing";
import { listUrls } from "@/api/urls";

type Period = "daily" | "monthly" | "yearly";

const PERIOD_DAYS: Record<Period, number> = { daily: 7, monthly: 30, yearly: 365 };
const PERIOD_LABELS: Record<Period, string> = { daily: "Daily", monthly: "Monthly", yearly: "Yearly" };

function formatLimit(value: number): string {
  return value < 0 ? "Unlimited" : String(value);
}

export default function DashboardPage() {
  const [period, setPeriod] = useState<Period>("monthly");

  const summaryQuery = useQuery({
    queryKey: ["analytics", "summary", PERIOD_DAYS[period]],
    queryFn: () => getOrgSummary(PERIOD_DAYS[period]),
  });
  const usageQuery = useQuery({ queryKey: ["billing", "summary"], queryFn: getUsageSummary });
  const recentLinksQuery = useQuery({
    queryKey: ["urls", "recent"],
    queryFn: () => listUrls({ page: 0, size: 5 }),
  });

  const summary = summaryQuery.data;
  const periodClicks = (summary?.series ?? []).reduce((sum, point) => sum + point.clicks, 0);
  const topLink = summary?.topLinks[0];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-h3 font-medium">Dashboard</h1>
        <p className="text-body2 text-muted-foreground">A live overview of your links and traffic.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader>
            <CardDescription>Total clicks</CardDescription>
            <CardTitle className="text-h4">{summary?.totalClicks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Total links</CardDescription>
            <CardTitle className="text-h4">{summary?.totalLinks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Clicks this period</CardDescription>
            <CardTitle className="text-h4">{summaryQuery.isLoading ? "—" : periodClicks}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Best performing link</CardDescription>
            <CardTitle className="text-h4">{topLink?.clicks ?? "—"}</CardTitle>
            {topLink && <p className="text-caption text-muted-foreground">{topLink.shortCode}</p>}
          </CardHeader>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle>Clicks over time</CardTitle>
            <CardDescription>Track how your links are performing.</CardDescription>
          </div>
          <div className="flex gap-1 rounded-md bg-muted p-1">
            {(Object.keys(PERIOD_LABELS) as Period[]).map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => setPeriod(key)}
                className={`rounded-sm px-3 py-1 text-body2 font-medium transition-colors ${
                  period === key ? "bg-card text-foreground shadow-[var(--shadow-card)]" : "text-muted-foreground"
                }`}
              >
                {PERIOD_LABELS[key]}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          {summaryQuery.isLoading ? (
            <p className="text-body2 text-muted-foreground">Loading…</p>
          ) : (
            <ClicksLineChart data={summary?.series ?? []} />
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Top performing links</CardTitle>
          </CardHeader>
          <CardContent>
            {(summary?.topLinks ?? []).length === 0 && (
              <p className="text-body2 text-muted-foreground">No clicks yet.</p>
            )}
            <ul className="divide-y divide-border/60">
              {(summary?.topLinks ?? []).map((link) => (
                <li key={link.shortCode} className="flex items-center justify-between gap-3 py-2 text-body2">
                  <Link to={`/links/${link.shortCode}`} className="font-medium text-primary hover:underline">
                    {link.shortCode}
                  </Link>
                  <span className="shrink-0 text-muted-foreground">{link.clicks} clicks</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Recent links</CardTitle>
          </CardHeader>
          <CardContent>
            {(recentLinksQuery.data?.items ?? []).length === 0 && (
              <p className="text-body2 text-muted-foreground">No links yet.</p>
            )}
            <ul className="divide-y divide-border/60">
              {(recentLinksQuery.data?.items ?? []).map((item) => (
                <li key={item.shortCode} className="py-2 text-body2">
                  <Link to={`/links/${item.shortCode}`} className="font-medium text-primary hover:underline">
                    {item.shortCode}
                  </Link>
                  <p className="truncate text-caption text-muted-foreground">{item.longUrl}</p>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Plan usage</CardTitle>
            <CardDescription>{usageQuery.data?.planName ?? "—"} plan</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {usageQuery.data && (
              <>
                <div className="space-y-1">
                  <div className="flex justify-between text-body2">
                    <span>Active links</span>
                    <span className="text-muted-foreground">
                      {usageQuery.data.activeLinks} / {formatLimit(usageQuery.data.maxLinks)}
                    </span>
                  </div>
                  {usageQuery.data.maxLinks >= 0 && (
                    <div className="h-2 rounded-full bg-muted">
                      <div
                        className="h-2 rounded-full bg-primary"
                        style={{
                          width: `${Math.min(100, Math.round((usageQuery.data.activeLinks / Math.max(usageQuery.data.maxLinks, 1)) * 100))}%`,
                        }}
                      />
                    </div>
                  )}
                </div>
                <Button asChild size="sm" variant="outline" className="w-full">
                  <Link to="/billing">Manage plan</Link>
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
