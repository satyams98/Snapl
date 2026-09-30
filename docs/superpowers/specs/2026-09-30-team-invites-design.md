# Team Invites (Members + Invite + Accept)

**Date:** 2026-09-30
**Status:** Approved for planning

## Context

The landing page (added in the September 2026 UI redesign) advertises "Team
roles — invite your team with Owner, Admin, and Member roles" as a feature.
The backend already fully supports organization membership and invitations
(`com.satyam.urlshortner.org` package: `OrgController`, `MembershipService`,
`Invitation`, `Membership`, `Role`), but there is **no frontend UI for it at
all** — an Owner has no way to actually invite anyone today.

Investigating the existing backend surfaced a real limitation that would make
a naively-built "accept invitation" flow silently broken for the most common
case (inviting someone brand new): `AuthService.issueTokensForCurrentOrg`
always scopes a user's session to the **first** organization they ever
joined (`findFirstByUserIdOrderByCreatedAtAsc`), never to one joined later
via an accepted invitation. `MembershipRepository`'s own comment confirms
this is a known, deliberate "Phase 1" simplification. This spec includes a
minimal backend fix alongside the frontend work, rather than shipping a UI
for a flow that doesn't actually work end-to-end.

## Goals

- Let an Owner/Admin see their org's members and invite new people by email
  and role.
- Let an invited person accept an invitation and end up in a session that is
  actually scoped to the org they were invited into.
- Keep the backend fix minimal: "your most recently joined org is your
  active org," not full multi-org switching.

## Non-goals

- No email delivery. The backend deliberately returns the raw invitation
  token only to the inviting Owner/Admin (see `InvitationResponse.java`'s
  existing comment) — the inviter shares the link manually. This spec does
  not add an email-sending integration.
- No multi-org switcher UI. A user still has exactly one "active" org per
  session; this spec only changes which org that is (most-recent membership
  instead of oldest) and makes accepting an invite actually apply it.
- No redirect-after-login plumbing. If an invitee isn't logged in yet, the
  accept page tells them to sign in/register and reopen the invite link —
  it does not thread a `?redirect=` param through login/register.
- No change to who can be invited as what role beyond a UI-level guard (see
  Frontend section) — the backend's existing role check is unchanged.
- No behavior changes to `AuthController`'s `logout` endpoint — it keeps
  clearing the same cookie with the same attributes. Its implementation is
  touched only to reuse the new shared component instead of its own private
  method (see `RefreshCookieIssuer` below), so the cookie name/path
  constants live in exactly one place.

## Backend changes

### `src/main/java/com/satyam/urlshortner/org/MembershipRepository.java`

Add a new derived query alongside the existing one:

```java
Mono<Membership> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
```

### `src/main/java/com/satyam/urlshortner/auth/AuthService.java`

- Change `issueTokensForCurrentOrg` to call
  `membershipRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())`
  instead of `...Asc(...)`. This is the one-line semantic change: from now
  on, login and refresh scope the session to whichever org the user joined
  **most recently**, not the first one.
- Rename the existing private `issueTokens(User, Membership, Organization)`
  to `issueTokensForMembership` and make it `public`, so
  `OrgController` (see below) can call it directly. Its two existing call
  sites (`register()` and `issueTokensForCurrentOrg()`) are updated to the
  new name; its signature and behavior are otherwise unchanged.

### New: `src/main/java/com/satyam/urlshortner/auth/RefreshCookieIssuer.java`

Extracts `AuthController`'s existing `withRefreshCookie` private method into
a reusable `@Component` so `OrgController` doesn't duplicate cookie
construction:

```java
@Component
public class RefreshCookieIssuer {
    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    public ResponseEntity<AuthResponse> issue(AuthResult result, ServerHttpResponse response) {
        response.addCookie(buildCookie(result.rawRefreshToken(), result.refreshTokenTtl()));
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(result.response());
    }

    public void clear(ServerHttpResponse response) {
        response.addCookie(buildCookie("", Duration.ZERO));
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }
}
```

`AuthController` injects this and replaces both its own private
`withRefreshCookie` (used by `register`/`login`/`refresh`) and `clearedCookie`
(used by `logout`) with calls to `refreshCookieIssuer.issue(...)` and
`refreshCookieIssuer.clear(response)` respectively. The cookie's name, path,
and attributes are unchanged — this only moves where the constants live, so
`logout`'s observable behavior (same cookie cleared the same way) does not
change.

### `src/main/java/com/satyam/urlshortner/org/MembershipService.java`

Change `acceptInvitation`'s return type from `Mono<MemberResponse>` to a new
small record `Mono<AcceptedInvitation>`:

```java
// New file: src/main/java/com/satyam/urlshortner/org/AcceptedInvitation.java
package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.User;

public record AcceptedInvitation(User user, Membership membership, Organization organization) {}
```

`acceptInvitation` keeps its existing validation logic (expiry, email match,
membership creation) but resolves and returns the `User` and `Organization`
alongside the new `Membership` instead of mapping to `MemberResponse`.

### `src/main/java/com/satyam/urlshortner/org/OrgController.java`

Change `acceptInvitation` from:

```java
@PostMapping("/invitations/{token}/accept")
public Mono<MemberResponse> acceptInvitation(@PathVariable String token) { ... }
```

to:

```java
@PostMapping("/invitations/{token}/accept")
public Mono<ResponseEntity<AuthResponse>> acceptInvitation(@PathVariable String token, ServerHttpResponse response) {
    return CurrentUser.get()
            .flatMap(principal -> membershipService.acceptInvitation(principal, token))
            .flatMap(accepted -> authService.issueTokensForMembership(accepted.user(), accepted.membership(), accepted.organization()))
            .map(result -> refreshCookieIssuer.issue(result, response));
}
```

`OrgController` gains two new constructor-injected dependencies:
`AuthService` and `RefreshCookieIssuer` (both already public/available;
`org` already depends on `auth` at the controller level via
`AuthPrincipal`/`CurrentUser`, so this isn't a new dependency direction).

**Net effect:** accepting an invitation now behaves exactly like login or
register from the frontend's point of view — it returns an `AuthResponse`
and sets a fresh refresh cookie, scoped to the org just joined.

## Frontend changes

### New: `frontend/src/api/org.ts`

```typescript
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

export function getCurrentOrg(): Promise<{ id: number; name: string; slug: string; yourRole: string }> {
  return apiFetch("/api/orgs/me");
}

export function listMembers(): Promise<OrgMember[]> {
  return apiFetch("/api/orgs/me/members");
}

export function inviteMember(email: string, role: "ADMIN" | "MEMBER"): Promise<Invitation> {
  return apiFetch("/api/orgs/me/invitations", { method: "POST", body: JSON.stringify({ email, role }) });
}
```

`acceptInvitation` is NOT in this file — it returns an `AuthResponse` and
sets the refresh cookie, so it belongs in `AuthContext` alongside
`login`/`register` (see below), following the existing pattern where
`api/client.ts`'s `apiFetch` is called directly from `AuthContext`, not
through a separate `api/auth.ts` module.

### `frontend/src/context/AuthContext.tsx`

Add one new method to the context, implemented the same way as `login` and
`register` (call the endpoint, `setAccessToken`, `setUser`):

```typescript
async function acceptInvitation(token: string) {
  const response = await apiFetch<AuthResponse>(`/api/orgs/invitations/${token}/accept`, { method: "POST" });
  setAccessToken(response.accessToken);
  setUser(toAuthUser(response));
}
```

Exposed on `AuthContextValue` and the provider's context value, alongside
the existing `login`/`register`/`logout`.

### New: `frontend/src/pages/TeamPage.tsx` (route: `/team`)

- Visible to all roles (matches the backend: `GET /members` has no role
  restriction).
- A card listing members: email, name, a role `Badge` (`OWNER`/`ADMIN` →
  `default` variant, `MEMBER` → `secondary`).
- An "Invite member" button, shown only when `user.role` (from `useAuth()`)
  is `"OWNER"` or `"ADMIN"` — this is a UI-level convenience guard; the
  backend independently enforces the same rule and returns 403 for anyone
  else, so hiding the button is not a security boundary, just avoids a
  confusing failed request.
- Clicking it opens a dialog (reusing the existing `Dialog` primitives, same
  pattern as `LinkFormDialog`) with an email `Input` and a role `Select`
  restricted to `Admin`/`Member` (not `Owner` — a UI-level choice; the
  backend's `InviteMemberRequest.role` accepts any `Role` value, but this
  spec doesn't add UI for inviting co-Owners).
- On successful invite, show the resulting invite link **once**, styled like
  the existing "Save your new API key now" reveal in `ApiKeysPage.tsx`:
  `${window.location.origin}/invitations/${invitation.token}` with a "Copy"
  button, plus a note that this link is the only way the invitee gets it
  (no email is sent).

### New: `frontend/src/pages/AcceptInvitationPage.tsx` (route: `/invitations/:token`, public)

- Public route (outside `ProtectedRoute`), same styling family as
  `UnlockPage.tsx` (centered card).
- If `useAuth().user` is present: shows the generic copy "You've been
  invited to join a team on Snapl" with an "Accept invitation" button. (The
  token alone doesn't expose the org's name without an extra backend lookup
  this spec doesn't add — showing a specific org/role greeting is a
  reasonable future enhancement, not required here.) Clicking the button
  calls `acceptInvitation(token)` from `AuthContext`, then navigates to
  `/dashboard` on success. Errors (expired, wrong email, already accepted,
  not found) surface via the same `ApiError` message pattern used elsewhere
  (`err instanceof ApiError ? err.message : "..."`).
- If `useAuth().user` is absent: shows "Sign in or create an account, then
  open this invite link again to accept it," with links to `/login` and
  `/register`. No redirect-back plumbing (non-goal, see above).

### `frontend/src/App.tsx`

Add two routes:
- `/invitations/:token` → `AcceptInvitationPage` (public, alongside
  `/unlock/:code`).
- `team` → `TeamPage` (inside the existing `ProtectedRoute`/`AppShell`
  group, alongside `domains`/`api-keys`/`billing`).

### `frontend/src/components/layout/Sidebar.tsx`

Add a `Team` entry to the existing "Organization" nav group (after Domains,
before API Keys — team management reads naturally next to domain
management), using the `Users` icon from `lucide-react` (already used
elsewhere in this codebase, e.g. the landing page's feature grid).

## Data flow summary

```
Owner/Admin on /team clicks "Invite member"
  -> POST /api/orgs/me/invitations {email, role}
  -> 200 Invitation {token, ...} shown once as a copyable link
  -> Owner/Admin shares the link out of band (Slack, email, etc.)

Invitee opens /invitations/:token
  not logged in -> told to sign in/register, then reopen the link
  logged in      -> clicks "Accept invitation"
                 -> POST /api/orgs/invitations/:token/accept
                 -> 200 AuthResponse (new access token, org now = the joined org)
                    + fresh refresh-token cookie
                 -> AuthContext updates user/token, navigate to /dashboard
                 -> invitee now sees the org's links/domains/analytics
```

## Error handling

- Backend: existing exceptions (`InvitationNotFoundException`,
  `InvitationExpiredException`, `InvitationEmailMismatchException`,
  `AccessDeniedForRoleException`) are unchanged — `GlobalExceptionHandler`
  already maps domain exceptions to problem-detail responses, confirmed by
  its existing use elsewhere in the codebase.
- Frontend: `AcceptInvitationPage` and the `TeamPage` invite dialog both
  render `err instanceof ApiError ? err.message : "<generic fallback>"`,
  matching every other form in this codebase (`LoginPage`, `DomainsPage`,
  `ApiKeysPage`, etc.) — no new error-handling pattern introduced.

## Verification

No automated test suite exists for the frontend (unchanged project
constraint). For the backend, this project does have a JUnit/Reactor test
suite (`UrlControllerTest.java`, `UrlServiceTest.java`, etc., 101 tests as of
this session, 100 passing standalone / 1 requiring a live Postgres via
`docker compose up -d postgres`); this change should get equivalent
coverage:
- A test that `issueTokensForCurrentOrg` (via login) picks the most recently
  created membership when a user has more than one.
- A test that `POST /api/orgs/invitations/{token}/accept` returns a 200 with
  an `AuthResponse` body and a `Set-Cookie` header, and that the returned
  token's org matches the invitation's org.
- Existing invitation validation tests (if any) continue to pass unchanged.

Manual verification (build + lint + live browser check, matching this
project's established pattern for frontend work): register two accounts,
invite the second one's email as Admin from the first account's `/team`
page, copy the link, open it as the second account, accept, and confirm the
second account's dashboard now shows the first account's org data (links,
domain count, etc.) rather than its own empty org.
