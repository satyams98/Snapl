import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";

export default function AcceptInvitationPage() {
  const { token = "" } = useParams<{ token: string }>();
  const { user, isLoading, acceptInvitation } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleAccept() {
    setError(null);
    setIsSubmitting(true);
    try {
      await acceptInvitation(token);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Unable to accept this invitation.");
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return <div className="flex min-h-svh items-center justify-center text-muted-foreground">Loading…</div>;
  }

  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>You've been invited</CardTitle>
          <CardDescription>
            {user ? "Accept to join this team on Snapl." : "Sign in or create an account to accept this invitation."}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {error && <p className="text-body2 text-destructive">{error}</p>}
        </CardContent>
        <CardFooter className="flex flex-col gap-3">
          {user ? (
            <Button className="w-full" disabled={isSubmitting} onClick={() => void handleAccept()}>
              {isSubmitting ? "Accepting…" : "Accept invitation"}
            </Button>
          ) : (
            <>
              <Button asChild className="w-full">
                <Link to="/login">Sign in</Link>
              </Button>
              <Button asChild className="w-full" variant="outline">
                <Link to="/register">Create an account</Link>
              </Button>
              <p className="text-center text-caption text-muted-foreground">
                After signing in, open this invite link again to accept it.
              </p>
            </>
          )}
        </CardFooter>
      </Card>
    </div>
  );
}
