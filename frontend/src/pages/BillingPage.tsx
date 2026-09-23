import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ApiError } from "@/api/client";
import { createCheckoutSession, getUsageSummary, listPlans } from "@/api/billing";

function formatLimit(value: number): string {
  return value < 0 ? "Unlimited" : String(value);
}

function UsageMeter({ label, used, limit }: { label: string; used: number; limit: number }) {
  const unlimited = limit < 0;
  const percent = unlimited ? 0 : Math.min(100, Math.round((used / Math.max(limit, 1)) * 100));
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-sm">
        <span>{label}</span>
        <span className="text-muted-foreground">
          {used} / {formatLimit(limit)}
        </span>
      </div>
      {!unlimited && (
        <div className="h-2 rounded-full bg-muted">
          <div className="h-2 rounded-full bg-primary" style={{ width: `${percent}%` }} />
        </div>
      )}
    </div>
  );
}

export default function BillingPage() {
  const [checkoutError, setCheckoutError] = useState<string | null>(null);

  const usageQuery = useQuery({ queryKey: ["billing", "summary"], queryFn: getUsageSummary });
  const plansQuery = useQuery({ queryKey: ["billing", "plans"], queryFn: listPlans });

  const checkoutMutation = useMutation({
    mutationFn: (planCode: string) => createCheckoutSession(planCode),
    onSuccess: (result) => {
      window.location.href = result.url;
    },
    onError: (err) => {
      setCheckoutError(
        err instanceof ApiError
          ? err.message
          : "Unable to start checkout. Please try again.",
      );
    },
  });

  const usage = usageQuery.data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-h3 font-medium">Billing &amp; plan</h1>
        <p className="text-sm text-muted-foreground">Manage your organization's plan and usage.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            {usage?.planName ?? "—"}
            {usage && (
              <Badge variant={usage.subscriptionStatus === "ACTIVE" ? "success" : usage.subscriptionStatus === "PAST_DUE" ? "warning" : "destructive"}>
                {usage.subscriptionStatus}
              </Badge>
            )}
          </CardTitle>
          <CardDescription>Current plan and usage this billing period.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {usage && (
            <>
              <UsageMeter label="Active links" used={usage.activeLinks} limit={usage.maxLinks} />
              <UsageMeter label="Custom domains" used={usage.customDomains} limit={usage.maxCustomDomains} />
              <p className="text-sm">
                <span className="text-muted-foreground">API access: </span>
                <Badge variant={usage.apiAccessAllowed ? "default" : "outline"}>
                  {usage.apiAccessAllowed ? "Enabled" : "Not available on this plan"}
                </Badge>
              </p>
            </>
          )}
        </CardContent>
      </Card>

      {checkoutError && (
        <Card className="border-destructive">
          <CardContent className="pt-6 text-sm text-destructive">{checkoutError}</CardContent>
        </Card>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {(plansQuery.data ?? []).map((plan) => {
          const isCurrent = usage?.planCode === plan.code;
          return (
            <Card key={plan.code} className={isCurrent ? "border-primary" : undefined}>
              <CardHeader>
                <CardTitle>{plan.name}</CardTitle>
                <CardDescription>
                  {plan.priceCents === 0 ? "Free" : `$${(plan.priceCents / 100).toFixed(2)}/mo`}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <p>{formatLimit(plan.maxLinks)} active links</p>
                <p>{formatLimit(plan.maxCustomDomains)} custom domains</p>
                <p>{formatLimit(plan.maxClicksPerMonth)} clicks/month</p>
                <p>{plan.apiAccessAllowed ? "API access included" : "No API access"}</p>
              </CardContent>
              <CardContent className="pt-0">
                <Button
                  className="w-full"
                  variant={isCurrent ? "outline" : "default"}
                  disabled={isCurrent || checkoutMutation.isPending}
                  onClick={() => {
                    setCheckoutError(null);
                    checkoutMutation.mutate(plan.code);
                  }}
                >
                  {isCurrent ? "Current plan" : "Upgrade"}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
