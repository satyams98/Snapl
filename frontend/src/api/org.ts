import { apiFetch } from "./client";

export interface OrgSummary {
  id: number;
  name: string;
  slug: string;
  yourRole: "OWNER" | "ADMIN" | "MEMBER";
}

export interface OrgMember {
  userId: number;
  email: string;
  name: string;
  role: "OWNER" | "ADMIN" | "MEMBER";
}

export interface Invitation {
  id: number;
  email: string;
  role: "OWNER" | "ADMIN" | "MEMBER";
  token: string;
  expiresAt: string;
}

export function getCurrentOrg(): Promise<OrgSummary> {
  return apiFetch<OrgSummary>("/api/orgs/me");
}

export function listMembers(): Promise<OrgMember[]> {
  return apiFetch<OrgMember[]>("/api/orgs/me/members");
}

export function inviteMember(email: string, role: "ADMIN" | "MEMBER"): Promise<Invitation> {
  return apiFetch<Invitation>("/api/orgs/me/invitations", {
    method: "POST",
    body: JSON.stringify({ email, role }),
  });
}
