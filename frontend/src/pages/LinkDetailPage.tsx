import { useState } from "react";
import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { getUrl } from "@/api/urls";
import { getLinkAnalytics } from "@/api/analytics";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ClicksLineChart } from "@/components/analytics/ClicksLineChart";
import { BreakdownList } from "@/components/analytics/BreakdownList";

type DetailTab = "overview" | "analytics";
const TABS: DetailTab[] = ["overview", "analytics"];

export default function LinkDetailPage() {
  const { code = "" } = useParams<{ code: string }>();
  const [tab, setTab] = useState<DetailTab>("overview");

  const urlQuery = useQuery({ queryKey: ["urls", code], queryFn: () => getUrl(code), enabled: Boolean(code) });
  const analyticsQuery = useQuery({
    queryKey: ["urls", code, "analytics"],
    queryFn: () => getLinkAnalytics(code, 30),
    enabled: Boolean(code) && tab === "analytics",
  });

  const link = urlQuery.data;

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold">{code}</h1>
        {link && <p className="text-sm text-muted-foreground">{link.longUrl}</p>}
      </div>

      <div role="tablist" aria-label="Link details" className="flex gap-2 border-b">
        {TABS.map((t) => (
          <button
            key={t}
            type="button"
            role="tab"
            aria-selected={tab === t}
            className={`border-b-2 px-3 py-2 text-sm font-medium capitalize transition-colors ${
              tab === t ? "border-primary text-foreground" : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>

      {tab === "overview" && (
        <Card>
          <CardHeader>
            <CardTitle>Overview</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2 text-sm">
            {urlQuery.isLoading && <p className="text-muted-foreground">Loading…</p>}
            {link && (
              <>
                <p>
                  <span className="text-muted-foreground">Short URL: </span>
                  <a href={link.shortUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">
                    {link.shortUrl}
                  </a>
                </p>
                <p>
                  <span className="text-muted-foreground">Status: </span>
                  <Badge variant={link.disabled ? "destructive" : "default"}>
                    {link.disabled ? "Disabled" : "Active"}
                  </Badge>
                  {link.passwordProtected && (
                    <Badge variant="outline" className="ml-2">
                      Password protected
                    </Badge>
                  )}
                  {link.startsAt && new Date(link.startsAt) > new Date() && (
                    <Badge variant="outline" className="ml-2">
                      Scheduled for {new Date(link.startsAt).toLocaleString()}
                    </Badge>
                  )}
                </p>
                <p>
                  <span className="text-muted-foreground">Folder: </span>
                  {link.folderName ?? "—"}
                </p>
                <div className="flex flex-wrap gap-1">
                  {link.tags.map((tag) => (
                    <Badge key={tag} variant="secondary">
                      {tag}
                    </Badge>
                  ))}
                </div>
              </>
            )}
          </CardContent>
        </Card>
      )}

      {tab === "analytics" && (
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Clicks over the last 30 days</CardTitle>
            </CardHeader>
            <CardContent>
              {analyticsQuery.isLoading ? (
                <p className="text-sm text-muted-foreground">Loading…</p>
              ) : (
                <ClicksLineChart data={analyticsQuery.data?.series ?? []} />
              )}
            </CardContent>
          </Card>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Top referrers</CardTitle>
              </CardHeader>
              <CardContent>
                <BreakdownList items={analyticsQuery.data?.referrers ?? []} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Devices</CardTitle>
              </CardHeader>
              <CardContent>
                <BreakdownList items={analyticsQuery.data?.devices ?? []} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Browsers</CardTitle>
              </CardHeader>
              <CardContent>
                <BreakdownList items={analyticsQuery.data?.browsers ?? []} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Operating systems</CardTitle>
              </CardHeader>
              <CardContent>
                <BreakdownList items={analyticsQuery.data?.operatingSystems ?? []} />
              </CardContent>
            </Card>
          </div>
        </div>
      )}
    </div>
  );
}
