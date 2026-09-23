import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import { createDomain, deleteDomain, listDomains, verifyDomain } from "@/api/domains";

export default function DomainsPage() {
  const [domain, setDomain] = useState("");
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const domainsQuery = useQuery({ queryKey: ["domains"], queryFn: listDomains });

  const createMutation = useMutation({
    mutationFn: () => createDomain(domain),
    onSuccess: () => {
      setDomain("");
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ["domains"] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Could not add that domain."),
  });

  const verifyMutation = useMutation({
    mutationFn: (id: number) => verifyDomain(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["domains"] }),
    onError: (err) => setError(err instanceof ApiError ? err.message : "Verification failed."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deleteDomain(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["domains"] }),
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    createMutation.mutate();
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Custom domains</h1>
        <p className="text-sm text-muted-foreground">
          Brand your short links with your own domain. TLS/SSL for verified domains is provisioned separately by
          your infrastructure team.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Add a domain</CardTitle>
          <CardDescription>Use a subdomain you control, e.g. go.yourcompany.com.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex items-end gap-3">
            <div className="flex-1 space-y-2">
              <Label htmlFor="domain">Domain</Label>
              <Input
                id="domain"
                required
                placeholder="go.yourcompany.com"
                value={domain}
                onChange={(e) => setDomain(e.target.value)}
              />
            </div>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Adding…" : "Add domain"}
            </Button>
          </form>
          {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
        </CardContent>
      </Card>

      <div className="space-y-4">
        {(domainsQuery.data ?? []).length === 0 && (
          <p className="text-sm text-muted-foreground">No custom domains yet.</p>
        )}
        {(domainsQuery.data ?? []).map((item) => (
          <Card key={item.id}>
            <CardHeader className="flex-row items-center justify-between space-y-0">
              <div>
                <CardTitle className="text-base">{item.domain}</CardTitle>
                <CardDescription>Added {new Date(item.createdAt).toLocaleDateString()}</CardDescription>
              </div>
              <Badge variant={item.verified ? "default" : "secondary"}>
                {item.verified ? "Verified" : "Pending verification"}
              </Badge>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              {!item.verified && (
                <div className="rounded-md border bg-muted/40 p-3">
                  <p className="text-muted-foreground">
                    Add a DNS <span className="font-medium">TXT</span> record for this domain, then verify:
                  </p>
                  <p className="mt-1 font-mono text-xs">
                    Name: {item.txtRecordName}
                    <br />
                    Value: {item.txtRecordValue}
                  </p>
                </div>
              )}
              <div className="flex gap-2">
                {!item.verified && (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={verifyMutation.isPending}
                    onClick={() => verifyMutation.mutate(item.id)}
                  >
                    Verify now
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={deleteMutation.isPending}
                  onClick={() => deleteMutation.mutate(item.id)}
                >
                  Remove
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
