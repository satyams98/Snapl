import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ClicksLineChart } from "@/components/analytics/ClicksLineChart";
import { getOrgSummary } from "@/api/analytics";

export default function AnalyticsPage() {
  const { data, isLoading } = useQuery({ queryKey: ["analytics", "summary"], queryFn: () => getOrgSummary(30) });

  return (
    <div className="space-y-6">
      <h1 className="text-h3 font-medium">Analytics</h1>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardDescription>Total clicks (all time)</CardDescription>
            <CardTitle className="text-h4">{data?.totalClicks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Active links</CardDescription>
            <CardTitle className="text-h4">{data?.totalLinks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Clicks over the last 30 days</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : (
            <ClicksLineChart data={data?.series ?? []} />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Top links</CardTitle>
        </CardHeader>
        <CardContent>
          {(data?.topLinks ?? []).length === 0 && <p className="text-sm text-muted-foreground">No clicks yet.</p>}
          <ul className="divide-y">
            {(data?.topLinks ?? []).map((link) => (
              <li key={link.shortCode} className="flex items-center justify-between gap-4 py-2 text-sm">
                <Link to={`/links/${link.shortCode}`} className="font-medium text-primary hover:underline">
                  {link.shortCode}
                </Link>
                <span className="flex-1 truncate text-muted-foreground">{link.longUrl}</span>
                <span className="shrink-0 font-medium">{link.clicks} clicks</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
