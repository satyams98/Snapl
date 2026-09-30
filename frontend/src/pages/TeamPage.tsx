import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/context/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { ApiError } from "@/api/client";
import { inviteMember, listMembers, type Invitation } from "@/api/org";

export default function TeamPage() {
  const { user } = useAuth();
  const canManageMembers = user?.role === "OWNER" || user?.role === "ADMIN";

  const [inviteOpen, setInviteOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<"ADMIN" | "MEMBER">("MEMBER");
  const [error, setError] = useState<string | null>(null);
  const [createdInvitation, setCreatedInvitation] = useState<Invitation | null>(null);

  const queryClient = useQueryClient();
  const membersQuery = useQuery({ queryKey: ["org", "members"], queryFn: listMembers });

  const inviteMutation = useMutation({
    mutationFn: () => inviteMember(email, role),
    onSuccess: (invitation) => {
      setCreatedInvitation(invitation);
      setEmail("");
      setError(null);
      void queryClient.invalidateQueries({ queryKey: ["org", "members"] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Could not send that invitation."),
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    inviteMutation.mutate();
  }

  function openInviteForm() {
    setCreatedInvitation(null);
    setError(null);
    setEmail("");
    setRole("MEMBER");
    setInviteOpen(true);
  }

  const inviteLink = createdInvitation ? `${window.location.origin}/invitations/${createdInvitation.token}` : null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-h3 font-medium">Team</h1>
          <p className="text-body2 text-muted-foreground">Manage who has access to your organization.</p>
        </div>
        {canManageMembers && !inviteOpen && <Button onClick={openInviteForm}>Invite member</Button>}
      </div>

      {inviteOpen && (
        <Card>
          <CardHeader>
            <CardTitle>Invite a team member</CardTitle>
            <CardDescription>They'll need to sign in with this email to accept.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {createdInvitation ? (
              <div className="space-y-3">
                <p className="text-body2">
                  Invitation created for <span className="font-medium">{createdInvitation.email}</span> as{" "}
                  {createdInvitation.role}. Share this link with them — it's the only way they'll get it:
                </p>
                <div className="flex items-center gap-2">
                  <code className="flex-1 truncate rounded-md bg-muted px-3 py-2 text-sm">{inviteLink}</code>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => inviteLink && void navigator.clipboard.writeText(inviteLink)}
                  >
                    Copy
                  </Button>
                </div>
                <Button size="sm" variant="ghost" onClick={() => setInviteOpen(false)}>
                  Done
                </Button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="flex flex-wrap items-end gap-4">
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    required
                    placeholder="teammate@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="role">Role</Label>
                  <Select id="role" value={role} onChange={(e) => setRole(e.target.value as "ADMIN" | "MEMBER")}>
                    <option value="MEMBER">Member</option>
                    <option value="ADMIN">Admin</option>
                  </Select>
                </div>
                <Button type="submit" disabled={inviteMutation.isPending}>
                  {inviteMutation.isPending ? "Sending…" : "Send invite"}
                </Button>
                <Button type="button" variant="ghost" onClick={() => setInviteOpen(false)}>
                  Cancel
                </Button>
              </form>
            )}
            {error && <p className="text-body2 text-destructive">{error}</p>}
          </CardContent>
        </Card>
      )}

      <div className="space-y-3">
        {membersQuery.isLoading && <p className="text-body2 text-muted-foreground">Loading…</p>}
        {(membersQuery.data ?? []).map((member) => (
          <Card key={member.userId}>
            <CardContent className="flex items-center justify-between gap-4 pt-6 text-sm">
              <div>
                <p className="font-medium">{member.name}</p>
                <p className="text-muted-foreground">{member.email}</p>
              </div>
              <Badge variant={member.role === "MEMBER" ? "secondary" : "default"}>{member.role}</Badge>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
