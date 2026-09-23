import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import { createApiKey, listApiKeys, revokeApiKey, type ApiKeyCreated } from "@/api/apiKeys";

export default function ApiKeysPage() {
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<string[]>(["READ"]);
  const [error, setError] = useState<string | null>(null);
  const [createdKey, setCreatedKey] = useState<ApiKeyCreated | null>(null);
  const queryClient = useQueryClient();

  const keysQuery = useQuery({ queryKey: ["api-keys"], queryFn: listApiKeys });

  const createMutation = useMutation({
    mutationFn: () => createApiKey(name, scopes),
    onSuccess: (created) => {
      setCreatedKey(created);
      setName("");
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ["api-keys"] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Could not create that key."),
  });

  const revokeMutation = useMutation({
    mutationFn: (id: number) => revokeApiKey(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ["api-keys"] }),
  });

  function toggleScope(scope: string) {
    setScopes((prev) => (prev.includes(scope) ? prev.filter((s) => s !== scope) : [...prev, scope]));
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    createMutation.mutate();
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-h3 font-medium">API keys</h1>
        <p className="text-sm text-muted-foreground">
          Use an API key to call the shortening API programmatically. Owners and admins can manage keys.
        </p>
      </div>

      {createdKey && (
        <Card className="border-primary">
          <CardHeader>
            <CardTitle className="text-base">Save your new API key now</CardTitle>
            <CardDescription>
              This is the only time the full key is shown. Store it somewhere safe.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex items-center gap-2">
              <code className="flex-1 truncate rounded-md bg-muted px-3 py-2 text-sm">{createdKey.apiKey}</code>
              <Button
                size="sm"
                variant="outline"
                onClick={() => void navigator.clipboard.writeText(createdKey.apiKey)}
              >
                Copy
              </Button>
            </div>
            <div>
              <p className="mb-1 text-xs text-muted-foreground">Example usage:</p>
              <pre className="overflow-x-auto rounded-md bg-muted px-3 py-2 text-xs">
                {`curl -X POST https://your-domain/shorten \\\n  -H "X-API-Key: ${createdKey.apiKey}" \\\n  -H "Content-Type: application/json" \\\n  -d '{"longUrl":"https://example.com"}'`}
              </pre>
            </div>
            <Button size="sm" variant="ghost" onClick={() => setCreatedKey(null)}>
              Done
            </Button>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Create a new key</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" required placeholder="CI pipeline" value={name} onChange={(e) => setName(e.target.value)} />
            </div>
            <div className="space-y-2">
              <span className="block text-sm font-medium">Scopes</span>
              <div className="flex gap-4">
                {["READ", "WRITE"].map((scope) => (
                  <label key={scope} className="flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={scopes.includes(scope)} onChange={() => toggleScope(scope)} />
                    {scope}
                  </label>
                ))}
              </div>
            </div>
            <Button type="submit" disabled={createMutation.isPending || scopes.length === 0}>
              {createMutation.isPending ? "Creating…" : "Create key"}
            </Button>
          </form>
          {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
        </CardContent>
      </Card>

      <div className="space-y-3">
        {(keysQuery.data ?? []).length === 0 && <p className="text-sm text-muted-foreground">No API keys yet.</p>}
        {(keysQuery.data ?? []).map((key) => (
          <Card key={key.id}>
            <CardContent className="flex items-center justify-between gap-4 pt-6 text-sm">
              <div>
                <p className="font-medium">{key.name}</p>
                <p className="font-mono text-xs text-muted-foreground">{key.keyPrefix}••••••••</p>
                <div className="mt-1 flex gap-1">
                  {key.scopes.map((scope) => (
                    <Badge key={scope} variant="secondary">
                      {scope}
                    </Badge>
                  ))}
                  {key.revoked && <Badge variant="destructive">Revoked</Badge>}
                </div>
              </div>
              {!key.revoked && (
                <Button
                  size="sm"
                  variant="ghost"
                  disabled={revokeMutation.isPending}
                  onClick={() => revokeMutation.mutate(key.id)}
                >
                  Revoke
                </Button>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
