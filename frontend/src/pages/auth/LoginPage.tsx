import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Zap } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DashboardPreviewMockup } from "@/components/marketing/DashboardPreviewMockup";

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await login(email, password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Unable to sign in. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm space-y-6">
          <div className="space-y-1 text-center lg:text-left">
            <h1 className="text-h3 font-medium">Sign in</h1>
            <p className="text-body2 text-muted-foreground">Access your organization's link dashboard.</p>
          </div>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            {error && <p className="text-body2 text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Signing in…" : "Sign in"}
            </Button>
          </form>
          <p className="text-center text-body2 text-muted-foreground lg:text-left">
            Don't have an account?{" "}
            <Link to="/register" className="font-medium text-primary underline-offset-4 hover:underline">
              Create one
            </Link>
          </p>
        </div>
      </div>
      <div className="hidden flex-col items-center justify-center gap-6 bg-primary-lighter p-12 lg:flex">
        <Link to="/" className="flex items-center gap-2">
          <div className="flex size-9 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Zap className="size-5" />
          </div>
          <span className="text-h5 font-medium">Snapl</span>
        </Link>
        <p className="max-w-sm text-center text-body1 text-muted-foreground">
          Branded short links, real-time analytics, and team access controls in one place.
        </p>
        <DashboardPreviewMockup />
      </div>
    </div>
  );
}
