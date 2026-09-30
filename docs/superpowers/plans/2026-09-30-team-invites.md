# Team Invites Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an Owner/Admin invite teammates by email and role, and let an invitee accept an invitation and land in a session actually scoped to the org they joined — closing the gap between the landing page's "Team roles" promise and the (currently nonexistent) frontend UI for it.

**Architecture:** A small backend fix changes which org a user's session is scoped to (most-recently-joined instead of first-ever-joined) and makes accepting an invitation immediately issue fresh tokens for the new org, exactly like login/register. Three new frontend pieces (an API module, a Team management page, and a public accept-invitation page) consume the existing/adjusted backend contract, following this codebase's established page patterns (`DomainsPage.tsx`, `ApiKeysPage.tsx`) and auth patterns (`AuthContext.tsx`).

**Tech Stack:** Java 21, Spring Boot 4 WebFlux/R2DBC (Mono/Flux), JUnit 5 + Mockito + Reactor `StepVerifier` for backend tests. React 19, TypeScript, TanStack Query, react-router-dom v7 for frontend (no automated frontend test suite — build/lint/manual verification, per this project's established pattern).

## Global Constraints

- Backend tests run with `JAVA_HOME` set to a JDK 21 install, e.g.:
  `JAVA_HOME="/c/Program Files/Java/jdk-21.0.12.1"` — every `mvn` command
  below assumes this is exported (prefix each command with
  `JAVA_HOME="/c/Program Files/Java/jdk-21.0.12.1" PATH="$JAVA_HOME/bin:$PATH"`
  if your shell doesn't already have it set). Run backend `mvn` commands from
  the repo root (`C:\Users\ssingh49\Downloads\urlshortner\urlshortner`).
  Run frontend `npm` commands from `frontend/`.
- No email delivery is added. The invite endpoint returns the raw token only
  to the inviting Owner/Admin; the frontend shows it once as a copyable link.
- No multi-org switcher UI. This plan only changes which single org is
  "active" for a session (most recently joined), not how to switch between
  multiple orgs.
- No redirect-after-login plumbing on the accept page — if not logged in, it
  tells the user to sign in/register and reopen the link.
- The invite-role dropdown only offers `Admin`/`Member` (not `Owner`) — a
  UI-level choice, not a backend restriction.
- Follow existing test conventions exactly: JUnit 5 + `@ExtendWith(MockitoExtension.class)` +
  `@Mock` fields + Reactor `StepVerifier` (see `AuthServiceTest.java`,
  `MembershipServiceTest.java`). No controller-level tests exist for
  `AuthController`/`OrgController` today — don't introduce a new testing
  style for them in this plan; verify controller wiring by compiling and by
  the manual end-to-end check in Task 7.
- No automated frontend test suite exists. Frontend tasks are verified by
  `npm run build`, `npm run lint`, and a manual browser check.

---

### Task 1: Switch active-org selection to most-recently-joined membership

**Files:**
- Modify: `src/main/java/com/satyam/urlshortner/org/MembershipRepository.java`
- Modify: `src/main/java/com/satyam/urlshortner/auth/AuthService.java`
- Modify: `src/test/java/com/satyam/urlshortner/auth/AuthServiceTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `AuthService.issueTokensForMembership(User, Membership, Organization): Mono<AuthResult>` —
  this is the renamed, now-`public`, former `issueTokens` method. Task 4
  calls this directly. Its behavior and signature (arguments in the same
  order) are otherwise unchanged from the current private `issueTokens`.

- [ ] **Step 1: Update the two affected tests to expect the new query method**

In `src/test/java/com/satyam/urlshortner/auth/AuthServiceTest.java`, change
both occurrences of `findFirstByUserIdOrderByCreatedAtAsc` to
`findFirstByUserIdOrderByCreatedAtDesc`:

In `loginFailsWhenUserHasNoMembership`, change:
```java
when(membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(1L)).thenReturn(Mono.empty());
```
to:
```java
when(membershipRepository.findFirstByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Mono.empty());
```

In `loginSucceedsAndIssuesTokens`, change:
```java
when(membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(1L)).thenReturn(Mono.just(membership));
```
to:
```java
when(membershipRepository.findFirstByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Mono.just(membership));
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q -Dtest=AuthServiceTest test`
Expected: `loginFailsWhenUserHasNoMembership` and `loginSucceedsAndIssuesTokens`
FAIL — production code still calls the `Asc` query, so the `Desc` stubs are
never matched (Mockito returns `null` for the unstubbed call, which surfaces
as a `NullPointerException` or wrong-exception failure in the `StepVerifier`
assertion).

- [ ] **Step 3: Rename the repository query in `MembershipRepository.java`**

Replace:
```java
    // A user's "current" org for login/refresh is the org they joined first (Phase 1 supports one active org per session).
    Mono<Membership> findFirstByUserIdOrderByCreatedAtAsc(Long userId);
```
with:
```java
    // A user's "current" org for login/refresh is the org they joined most recently — this makes
    // accepting an invitation into a new org actually take effect on the next login/refresh.
    Mono<Membership> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
```

(This is a straight rename — nothing else in the codebase calls the `Asc`
query, so there is no other query method to keep.)

- [ ] **Step 4: Update `AuthService.java` to use the renamed query and expose token issuance**

Change the call inside `issueTokensForCurrentOrg`:
```java
    private Mono<AuthResult> issueTokensForCurrentOrg(User user) {
        return membershipRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId())
```
to:
```java
    private Mono<AuthResult> issueTokensForCurrentOrg(User user) {
        return membershipRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
```

Rename the existing private method and make it public — change:
```java
    private Mono<AuthResult> issueTokens(User user, Membership membership, Organization org) {
```
to:
```java
    public Mono<AuthResult> issueTokensForMembership(User user, Membership membership, Organization org) {
```

Update its two call sites in the same file — in `register()`, change:
```java
                .flatMap(org -> createUser(request).flatMap(user -> createMembership(user, org, Role.OWNER)
                        .flatMap(membership -> issueTokens(user, membership, org))));
```
to:
```java
                .flatMap(org -> createUser(request).flatMap(user -> createMembership(user, org, Role.OWNER)
                        .flatMap(membership -> issueTokensForMembership(user, membership, org))));
```

In `issueTokensForCurrentOrg()`, change:
```java
                .flatMap(membership -> organizationRepository.findById(membership.getOrgId())
                        .flatMap(org -> issueTokens(user, membership, org)));
```
to:
```java
                .flatMap(membership -> organizationRepository.findById(membership.getOrgId())
                        .flatMap(org -> issueTokensForMembership(user, membership, org)));
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q -Dtest=AuthServiceTest test`
Expected: PASS (all tests in `AuthServiceTest`, including the two updated
ones).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/satyam/urlshortner/org/MembershipRepository.java \
  src/main/java/com/satyam/urlshortner/auth/AuthService.java \
  src/test/java/com/satyam/urlshortner/auth/AuthServiceTest.java
git commit -m "fix(auth): scope sessions to the most recently joined org, not the first"
```

---

### Task 2: Extract refresh-cookie handling into a reusable component

**Files:**
- Create: `src/main/java/com/satyam/urlshortner/auth/RefreshCookieIssuer.java`
- Create: `src/test/java/com/satyam/urlshortner/auth/RefreshCookieIssuerTest.java`
- Modify: `src/main/java/com/satyam/urlshortner/auth/AuthController.java` (full rewrite)

**Interfaces:**
- Consumes: `AuthResult` (existing, from `AuthService`), `AuthResponse` (existing).
- Produces: `RefreshCookieIssuer` with `issue(AuthResult, ServerHttpResponse): ResponseEntity<AuthResponse>`
  and `clear(ServerHttpResponse): void`, plus a `public static final String REFRESH_COOKIE_NAME`
  constant. Task 4's `OrgController` change consumes both `issue(...)` and
  this constant is not needed there, only `issue(...)`.

- [ ] **Step 1: Write the failing test for the new component**

Create `src/test/java/com/satyam/urlshortner/auth/RefreshCookieIssuerTest.java`:

```java
package com.satyam.urlshortner.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshCookieIssuerTest {

    @Mock
    private ServerHttpResponse response;

    private final RefreshCookieIssuer issuer = new RefreshCookieIssuer();

    @Test
    void issueSetsCookieAndReturnsBody() {
        AuthResponse body = new AuthResponse("access-token", 900, 1L, "owner@acme.test", 10L, "Acme", "OWNER");
        AuthResult result = new AuthResult(body, "raw-refresh-token", Duration.ofDays(30));

        ResponseEntity<AuthResponse> entity = issuer.issue(result, response);

        ArgumentCaptor<ResponseCookie> captor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(response).addCookie(captor.capture());
        ResponseCookie cookie = captor.getValue();

        assertEquals(RefreshCookieIssuer.REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("raw-refresh-token", cookie.getValue());
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.isSecure());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals(Duration.ofDays(30), cookie.getMaxAge());
        assertEquals(body, entity.getBody());
    }

    @Test
    void clearSetsEmptyExpiredCookie() {
        issuer.clear(response);

        ArgumentCaptor<ResponseCookie> captor = ArgumentCaptor.forClass(ResponseCookie.class);
        verify(response).addCookie(captor.capture());
        ResponseCookie cookie = captor.getValue();

        assertEquals(RefreshCookieIssuer.REFRESH_COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q -Dtest=RefreshCookieIssuerTest test`
Expected: FAIL with a compile error — `RefreshCookieIssuer` does not exist yet.

- [ ] **Step 3: Create `RefreshCookieIssuer.java`**

```java
package com.satyam.urlshortner.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshCookieIssuer {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
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

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q -Dtest=RefreshCookieIssuerTest test`
Expected: PASS (both tests).

- [ ] **Step 5: Replace `AuthController.java` to use the new component**

```java
package com.satyam.urlshortner.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieIssuer refreshCookieIssuer;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, ServerHttpResponse response) {
        return authService.register(request).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request, ServerHttpResponse response) {
        return authService.login(request.email(), request.password()).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@CookieValue(RefreshCookieIssuer.REFRESH_COOKIE_NAME) String refreshToken, ServerHttpResponse response) {
        return authService.refresh(refreshToken).map(result -> refreshCookieIssuer.issue(result, response));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@CookieValue(name = RefreshCookieIssuer.REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                              ServerHttpResponse response) {
        Mono<Void> revoke = refreshToken != null ? authService.logout(refreshToken) : Mono.empty();
        return revoke.then(Mono.fromSupplier(() -> {
            refreshCookieIssuer.clear(response);
            return ResponseEntity.noContent().<Void>build();
        }));
    }
}
```

- [ ] **Step 6: Run the full auth test package to confirm nothing broke**

Run: `mvn -q -Dtest=com.satyam.urlshortner.auth.* test`
Expected: PASS (all tests in the `auth` package, including
`AuthServiceTest`, `RefreshCookieIssuerTest`, `JwtServiceTest`,
`ApiKeyAuthenticationManagerTest`).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/satyam/urlshortner/auth/RefreshCookieIssuer.java \
  src/test/java/com/satyam/urlshortner/auth/RefreshCookieIssuerTest.java \
  src/main/java/com/satyam/urlshortner/auth/AuthController.java
git commit -m "refactor(auth): extract refresh-cookie handling into RefreshCookieIssuer"
```

---

### Task 3: Make `acceptInvitation` return the accepted user/membership/org

**Files:**
- Create: `src/main/java/com/satyam/urlshortner/org/AcceptedInvitation.java`
- Modify: `src/main/java/com/satyam/urlshortner/org/MembershipService.java`
- Modify: `src/test/java/com/satyam/urlshortner/org/MembershipServiceTest.java`

**Interfaces:**
- Consumes: nothing new (uses `MembershipService`'s existing injected
  `organizationRepository`, `membershipRepository`, `invitationRepository`,
  `userRepository`).
- Produces: `AcceptedInvitation(User user, Membership membership, Organization organization)`
  and `MembershipService.acceptInvitation(AuthPrincipal, String): Mono<AcceptedInvitation>`
  (changed from `Mono<MemberResponse>`). Task 4's `OrgController` consumes
  this exact type and its three accessor methods (`.user()`, `.membership()`,
  `.organization()`).

- [ ] **Step 1: Update the existing success test to expect the new return type**

In `src/test/java/com/satyam/urlshortner/org/MembershipServiceTest.java`,
replace `acceptInvitationCreatesMembershipOnSuccess`:

```java
    @Test
    void acceptInvitationCreatesMembershipOnSuccess() {
        Invitation invitation = new Invitation(1L, 10L, MEMBER.email(), Role.MEMBER, "tok", 2L,
                Instant.now().plusSeconds(3600), null, Instant.now());
        User user = new User(MEMBER.userId(), MEMBER.email(), "hashed", "Member", Instant.now());

        when(invitationRepository.findByToken("tok")).thenReturn(Mono.just(invitation));
        when(membershipRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 55L)));
        when(invitationRepository.acceptByToken(eq("tok"), any())).thenReturn(Mono.just(1));
        when(userRepository.findById(MEMBER.userId())).thenReturn(Mono.just(user));

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "tok"))
                .assertNext(response -> assertEquals(Role.MEMBER, response.role()))
                .verifyComplete();
    }
```

with:

```java
    @Test
    void acceptInvitationCreatesMembershipOnSuccess() {
        Invitation invitation = new Invitation(1L, 10L, MEMBER.email(), Role.MEMBER, "tok", 2L,
                Instant.now().plusSeconds(3600), null, Instant.now());
        User user = new User(MEMBER.userId(), MEMBER.email(), "hashed", "Member", Instant.now());
        Organization org = new Organization(10L, "Acme", "acme", Instant.now());

        when(invitationRepository.findByToken("tok")).thenReturn(Mono.just(invitation));
        when(membershipRepository.save(any())).thenAnswer(inv -> Mono.just(withId(inv.getArgument(0), 55L)));
        when(invitationRepository.acceptByToken(eq("tok"), any())).thenReturn(Mono.just(1));
        when(userRepository.findById(MEMBER.userId())).thenReturn(Mono.just(user));
        when(organizationRepository.findById(10L)).thenReturn(Mono.just(org));

        StepVerifier.create(membershipService.acceptInvitation(MEMBER, "tok"))
                .assertNext(accepted -> {
                    assertEquals(Role.MEMBER, accepted.membership().getRole());
                    assertEquals("Acme", accepted.organization().getName());
                    assertEquals(MEMBER.email(), accepted.user().getEmail());
                })
                .verifyComplete();
    }
```

- [ ] **Step 2: Run the tests to verify the updated one fails**

Run: `mvn -q -Dtest=MembershipServiceTest test`
Expected: FAIL with a compile error — `AcceptedInvitation` does not exist
yet and `Mono<MemberResponse>` has no `.membership()`/`.organization()`
methods.

- [ ] **Step 3: Create `AcceptedInvitation.java`**

```java
package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.User;

public record AcceptedInvitation(User user, Membership membership, Organization organization) {}
```

- [ ] **Step 4: Update `acceptInvitation` in `MembershipService.java`**

Replace:
```java
    public Mono<MemberResponse> acceptInvitation(AuthPrincipal principal, String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .flatMap(invitation -> validateInvitation(invitation, principal))
                .flatMap(invitation -> membershipRepository.save(new Membership(null, principal.userId(), invitation.getOrgId(), invitation.getRole(), Instant.now()))
                        .flatMap(membership -> invitationRepository.acceptByToken(token, Instant.now())
                                .then(userRepository.findById(principal.userId()))
                                .map(user -> new MemberResponse(user.getId(), user.getEmail(), user.getName(), membership.getRole()))));
    }
```

with:
```java
    public Mono<AcceptedInvitation> acceptInvitation(AuthPrincipal principal, String token) {
        return invitationRepository.findByToken(token)
                .switchIfEmpty(Mono.error(new InvitationNotFoundException(token)))
                .flatMap(invitation -> validateInvitation(invitation, principal))
                .flatMap(invitation -> membershipRepository.save(new Membership(null, principal.userId(), invitation.getOrgId(), invitation.getRole(), Instant.now()))
                        .flatMap(membership -> invitationRepository.acceptByToken(token, Instant.now())
                                .then(Mono.zip(
                                        userRepository.findById(principal.userId()),
                                        organizationRepository.findById(invitation.getOrgId())))
                                .map(tuple -> new AcceptedInvitation(tuple.getT1(), membership, tuple.getT2()))));
    }
```

(`organizationRepository` is already a constructor-injected field on this
class, used by `getCurrentOrganization` — no new dependency needed.)

- [ ] **Step 5: Run the tests to verify they pass**

Run: `mvn -q -Dtest=MembershipServiceTest test`
Expected: PASS (all tests in `MembershipServiceTest`, including the 4
unchanged error-path tests and the updated success test).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/satyam/urlshortner/org/AcceptedInvitation.java \
  src/main/java/com/satyam/urlshortner/org/MembershipService.java \
  src/test/java/com/satyam/urlshortner/org/MembershipServiceTest.java
git commit -m "feat(org): return accepted user/membership/org from acceptInvitation"
```

---

### Task 4: Wire `OrgController` to issue fresh tokens on accept

**Files:**
- Modify: `src/main/java/com/satyam/urlshortner/org/OrgController.java`

**Interfaces:**
- Consumes: `AuthService.issueTokensForMembership(User, Membership, Organization): Mono<AuthResult>`
  (Task 1), `RefreshCookieIssuer.issue(AuthResult, ServerHttpResponse): ResponseEntity<AuthResponse>`
  (Task 2), `MembershipService.acceptInvitation(...): Mono<AcceptedInvitation>` (Task 3).
- Produces: `POST /api/orgs/invitations/{token}/accept` now returns
  `ResponseEntity<AuthResponse>` with a fresh refresh-token cookie, instead
  of a bare `MemberResponse` — Task 7's frontend `AcceptInvitationPage`
  depends on this exact response shape (same as login/register).

There is no existing controller-level test for `OrgController` to extend —
this task is verified by a successful build; end-to-end behavior is verified
manually once the frontend exists (Task 7).

- [ ] **Step 1: Replace `OrgController.java`**

```java
package com.satyam.urlshortner.org;

import com.satyam.urlshortner.auth.AuthResponse;
import com.satyam.urlshortner.auth.AuthService;
import com.satyam.urlshortner.auth.CurrentUser;
import com.satyam.urlshortner.auth.RefreshCookieIssuer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final MembershipService membershipService;
    private final AuthService authService;
    private final RefreshCookieIssuer refreshCookieIssuer;

    @GetMapping("/me")
    public Mono<OrganizationResponse> currentOrganization() {
        return CurrentUser.get().flatMap(membershipService::getCurrentOrganization);
    }

    @GetMapping("/me/members")
    public Flux<MemberResponse> members() {
        return CurrentUser.get().flatMapMany(membershipService::listMembers);
    }

    @PostMapping("/me/invitations")
    public Mono<InvitationResponse> invite(@Valid @RequestBody InviteMemberRequest request) {
        return CurrentUser.get().flatMap(principal -> membershipService.inviteMember(principal, request));
    }

    @PostMapping("/invitations/{token}/accept")
    public Mono<ResponseEntity<AuthResponse>> acceptInvitation(@PathVariable String token, ServerHttpResponse response) {
        return CurrentUser.get()
                .flatMap(principal -> membershipService.acceptInvitation(principal, token))
                .flatMap(accepted -> authService.issueTokensForMembership(accepted.user(), accepted.membership(), accepted.organization()))
                .map(result -> refreshCookieIssuer.issue(result, response));
    }
}
```

- [ ] **Step 2: Build to confirm it compiles and wires correctly**

Run: `mvn -q -DskipTests package`
Expected: BUILD SUCCESS (this exercises Spring's constructor-injection
wiring for the two new `OrgController` dependencies at bean-definition time
during annotation processing; full context startup with a live database is
covered separately by `UrlshortnerApplicationTests`, which needs
`docker compose up -d postgres` running — not required for this step).

- [ ] **Step 3: Run the full backend test suite**

Run: `mvn -q test`
Expected: 100/101 tests pass (the one pre-existing exception is
`UrlshortnerApplicationTests.contextLoads`, which needs a live Postgres via
`docker compose up -d postgres redis kafka` — not a regression from this
change; confirm no *new* failures appear beyond that one).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/satyam/urlshortner/org/OrgController.java
git commit -m "feat(org): accepting an invitation now issues a fresh session for the joined org"
```

---

### Task 5: Frontend org API client + `AuthContext.acceptInvitation`

**Files:**
- Create: `frontend/src/api/org.ts`
- Modify: `frontend/src/context/AuthContext.tsx`

**Interfaces:**
- Consumes: `apiFetch` (existing, from `@/api/client`), `setAccessToken`
  (existing, from `@/api/tokenStore`).
- Produces: `getCurrentOrg(): Promise<OrgSummary>`, `listMembers(): Promise<OrgMember[]>`,
  `inviteMember(email: string, role: "ADMIN" | "MEMBER"): Promise<Invitation>`
  — Task 6's `TeamPage` imports `listMembers`, `inviteMember`, and the
  `Invitation`/`OrgMember` types from this file. `useAuth().acceptInvitation(token: string): Promise<void>` —
  Task 7's `AcceptInvitationPage` calls this.

- [ ] **Step 1: Create `frontend/src/api/org.ts`**

```typescript
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
```

- [ ] **Step 2: Add `acceptInvitation` to `AuthContext.tsx`**

Change the `AuthContextValue` interface from:
```tsx
interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string, organizationName: string) => Promise<void>;
  logout: () => Promise<void>;
}
```
to:
```tsx
interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string, organizationName: string) => Promise<void>;
  logout: () => Promise<void>;
  acceptInvitation: (token: string) => Promise<void>;
}
```

Add a new function inside `AuthProvider`, right after the existing `register` function:
```tsx
  async function acceptInvitation(token: string) {
    const response = await apiFetch<AuthResponse>(`/api/orgs/invitations/${token}/accept`, {
      method: "POST",
    });
    setAccessToken(response.accessToken);
    setUser(toAuthUser(response));
  }
```

Change the provider's context value from:
```tsx
  return <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>{children}</AuthContext.Provider>;
```
to:
```tsx
  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout, acceptInvitation }}>
      {children}
    </AuthContext.Provider>
  );
```

- [ ] **Step 3: Build and lint**

Run (from `frontend/`): `npm run build`
Expected: succeeds with no errors.

Run: `npm run lint`
Expected: no new errors (same pre-existing warning baseline as before this
task).

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/org.ts frontend/src/context/AuthContext.tsx
git commit -m "feat(org): add org API client and AuthContext.acceptInvitation"
```

---

### Task 6: Team page (members list + invite dialog)

**Files:**
- Create: `frontend/src/pages/TeamPage.tsx`
- Modify: `frontend/src/components/layout/Sidebar.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `listMembers`, `inviteMember`, `OrgMember`, `Invitation` from
  `@/api/org` (Task 5). `useAuth()` (existing, now also exposes
  `acceptInvitation` from Task 5, unused on this page). `Card`, `Badge`,
  `Button`, `Input`, `Label`, `Select` (all existing UI primitives). `ApiError`
  (existing, from `@/api/client`).
- Produces: `TeamPage` default export, mounted at `/team`.

- [ ] **Step 1: Create `frontend/src/pages/TeamPage.tsx`**

```tsx
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
```

- [ ] **Step 2: Add a "Team" nav item to `Sidebar.tsx`**

Change the import line:
```tsx
import {
  BarChart3,
  CreditCard,
  Globe,
  KeyRound,
  LayoutDashboard,
  Link2,
  PanelLeftClose,
  PanelLeftOpen,
  Zap,
} from "lucide-react";
```
to:
```tsx
import {
  BarChart3,
  CreditCard,
  Globe,
  KeyRound,
  LayoutDashboard,
  Link2,
  PanelLeftClose,
  PanelLeftOpen,
  Users,
  Zap,
} from "lucide-react";
```

Change the "Organization" group's items from:
```tsx
    items: [
      { to: "/domains", label: "Domains", icon: Globe },
      { to: "/api-keys", label: "API Keys", icon: KeyRound },
      { to: "/billing", label: "Billing", icon: CreditCard },
    ],
```
to:
```tsx
    items: [
      { to: "/domains", label: "Domains", icon: Globe },
      { to: "/team", label: "Team", icon: Users },
      { to: "/api-keys", label: "API Keys", icon: KeyRound },
      { to: "/billing", label: "Billing", icon: CreditCard },
    ],
```

- [ ] **Step 3: Add the `/team` route in `App.tsx`**

Add the import alongside the other page imports:
```tsx
import TeamPage from "@/pages/TeamPage";
```

Add the route inside the existing `<Route element={<AppShell />}>` block,
right after the `domains` route:
```tsx
                <Route path="domains" element={<DomainsPage />} />
                <Route path="team" element={<TeamPage />} />
```

- [ ] **Step 4: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 5: Manual check**

Run `npm run dev`, log in, click "Team" in the sidebar. Confirm: you (the
Owner) appear in the member list with an "OWNER" badge; the "Invite member"
button is visible (you're an Owner); clicking it shows the email/role form;
submitting invites successfully shows a one-time copyable link in the
format `http://localhost:5173/invitations/<token>`.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/TeamPage.tsx frontend/src/components/layout/Sidebar.tsx frontend/src/App.tsx
git commit -m "feat(team): add Team page with member list and invite dialog"
```

---

### Task 7: Accept-invitation page

**Files:**
- Create: `frontend/src/pages/AcceptInvitationPage.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `useAuth()` (now including `acceptInvitation`, from Task 5),
  `ApiError` (existing), `Card`/`CardHeader`/`CardTitle`/`CardDescription`/`CardContent`/`CardFooter`,
  `Button` (existing).
- Produces: `AcceptInvitationPage` default export, mounted at
  `/invitations/:token` (public route).

- [ ] **Step 1: Create `frontend/src/pages/AcceptInvitationPage.tsx`**

```tsx
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
```

- [ ] **Step 2: Add the `/invitations/:token` route in `App.tsx`**

Add the import alongside the other page imports:
```tsx
import AcceptInvitationPage from "@/pages/AcceptInvitationPage";
```

Add the route as a public, top-level route, right after `/unlock/:code`:
```tsx
            <Route path="/unlock/:code" element={<UnlockPage />} />
            <Route path="/invitations/:token" element={<AcceptInvitationPage />} />
```

- [ ] **Step 3: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 4: End-to-end manual verification**

This is the full verification for the whole feature — run it with both the
backend (`docker compose up -d postgres redis kafka` then
`./mvnw spring-boot:run`, or the equivalent `mvn` invocation with
`JAVA_HOME` set) and frontend (`npm run dev`) running:

1. Register Account A (e.g. `owner@acceptance-test.dev`, org "Acceptance A").
2. Create a link on Account A's `/links` page so its org has visible data.
3. Register Account B in a different browser/incognito window (e.g.
   `invitee@acceptance-test.dev`, org "Acceptance B" — this becomes their
   first/solo org).
4. As Account A, go to `/team`, invite `invitee@acceptance-test.dev` as
   Admin, copy the invite link.
5. As Account B (already logged in from step 3), open the copied invite
   link. Confirm the page shows "Accept invitation" (not the signed-out
   copy). Click it.
6. Confirm you land on `/dashboard` and it shows **Account A's org data**
   (the link created in step 2, org name "Acceptance A") — not Account B's
   own empty org. This confirms the Task 1 fix actually took effect: Account
   B's session is now scoped to the most-recently-joined org.
7. As Account A, revisit `/team` — confirm Account B now appears in the
   member list with role "Admin".

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/AcceptInvitationPage.tsx frontend/src/App.tsx
git commit -m "feat(team): add public accept-invitation page"
```

---

## Self-Review Notes

- **Spec coverage:** backend org-selection fix + token rename (Task 1) →
  shared cookie component (Task 2) → `acceptInvitation` return type (Task 3)
  → `OrgController` wiring (Task 4) → frontend API client + `AuthContext`
  (Task 5) → Team page (Task 6) → accept page (Task 7). Every section of the
  spec (Backend changes, Frontend changes, Data flow, Error handling) maps
  to a task. Error handling is explicitly unchanged (spec's own point) so no
  separate task was needed for it — verified by leaving
  `GlobalExceptionHandler.java` untouched, confirmed already covering all 4
  invitation-related exceptions plus `AccessDeniedForRoleException`.
- **Type consistency:** `AcceptedInvitation(User, Membership, Organization)`
  defined in Task 3 is consumed with the exact same accessor names
  (`.user()`, `.membership()`, `.organization()`) in Task 4.
  `issueTokensForMembership(User, Membership, Organization)` defined in
  Task 1 is called with arguments in that same order in Task 4.
  `AuthContextValue.acceptInvitation: (token: string) => Promise<void>`
  defined in Task 5 is consumed identically in Task 7's
  `acceptInvitation(token)` call — no signature drift.
- **No placeholders:** every step has complete code or an exact find/replace
  snippet; no "add error handling" or "TBD" markers.
