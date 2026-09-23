# UI/UX Redesign: "Snapl" (SaasAble-inspired)

**Date:** 2026-09-23
**Status:** Approved for planning

## Context

The URL shortener backend is production-featured (multi-tenant orgs, custom domains,
Kafka-backed click analytics, Redis rate limiting, Stripe billing, API keys), but the
frontend is a bare, unbranded Tailwind/shadcn shell with no landing page — `/` drops
straight into an authenticated links table.

The user wants a modern UI/UX redesign, including a public landing page, inspired by
the SaasAble admin dashboard template (`free.admin.saasable.io`), which they provided
via screenshots (dashboard, chart widget, auth split-screen, color tokens, type scale)
since the site itself is blocked by org browsing policy.

## Goals

- Give the product a name and visual identity ("Snapl").
- Add a public marketing landing page.
- Add an authenticated Dashboard home page (KPIs + chart + widgets), SaasAble-style.
- Restyle the existing app shell and all existing pages to a cohesive new design system.
- Keep it real: every number/widget on screen must come from an existing backend
  endpoint. No fabricated metrics, customer logos, testimonials, or non-functional
  decorative UI (fake notification bells, fake search, etc).

## Non-goals

- No migration to MUI. SaasAble is MUI-based; we recreate its *visual language*
  (tokens, layout, type) on top of the existing Tailwind v4 + Radix/shadcn stack.
- No backend changes. Pricing on the landing page is static copy sourced from the
  real seed data in `V13__billing.sql`, not a live API call (that endpoint requires
  auth today, and adding a public one is out of scope).
- No notifications feature, no RTL/i18n, no settings/profile page — none of these
  exist in the backend today, so no UI is built for them.

## Design tokens (`frontend/src/index.css`)

Replace the current default shadcn grayscale palette. Keep the existing
`@theme inline` var-mapping structure — components keep referencing
`bg-primary`, `text-muted-foreground`, etc. Only the CSS variable *values* change,
plus new semantic tokens are added.

### Colors (light)

| Token | Hex | Usage |
|---|---|---|
| `--primary` | `#606BDF` | primary actions, active nav, links |
| `--primary-lighter` | `#E0E0FF` | primary subtle background (badges, active nav bg) |
| `--primary-dark` | `#3944B8` | hover/active state |
| `--secondary` | `#5A5C78` | secondary text/accents |
| `--background` | `#FBF8FF` | page background |
| `--card` | `#FFFFFF` | card surfaces |
| `--muted` | `#F5F2FA` | muted backgrounds (table header, subtle fills) |
| `--muted-foreground` | `#46464F` | secondary text |
| `--foreground` | `#1B1B1F` | primary text |
| `--border` | `#C7C5D0` | hairline borders |
| `--success` | `#22892F` / lighter `#C8FFC0` | active/verified states |
| `--warning` | `#AE6600` / lighter `#FFEEE1` | pending states |
| `--info` | `#008394` / lighter `#D4F7FF` | informational badges |
| `--destructive` | `#DE3730` / lighter `#FFEDEA` | disabled/error states |

### Colors (dark)

Derive from the same hue families, following the existing dark-mode pattern already
in `index.css` (invert lightness, keep hue): background near `#171A31`/`#1B1B1F`,
card `#232336`, foreground `#F5F2FA`, primary stays `#606BDF`/lighter `#8890EA` for
sufficient contrast, semantic colors get their `-darker` container values as
backgrounds with `-lighter` as text.

### Manual theme toggle

Currently dark mode follows `@media (prefers-color-scheme: dark)` only. Add:
- A `data-theme="dark" | "light"` attribute on `<html>`, set by a new `useTheme()`
  hook (`frontend/src/hooks/useTheme.ts`) that persists to `localStorage`
  (`snapl-theme`) and defaults to OS preference when unset.
- Restructure `index.css` dark tokens under
  `:root[data-theme="dark"], :root:not([data-theme="light"]) @media (prefers-color-scheme: dark)`
  so an explicit choice overrides OS preference, matching standard shadcn dark-mode
  setup.
- A sun/moon icon button in the Topbar (and in the landing page nav) calling
  `useTheme().toggle()`.

### Typography

Add Inter via Google Fonts link in `index.html`. Add Tailwind v4 `@theme` font-size
tokens (value + paired line-height) so `text-h1`…`text-h6`, `text-subtitle1/2`,
`text-body1/2`, `text-caption` utilities exist:

| Utility | Size / Line-height | Weight |
|---|---|---|
| `text-h1` | 40px / 44px | 500 |
| `text-h2` | 32px / 36px | 500 |
| `text-h3` | 28px / 32px | 500 |
| `text-h4` | 24px / 28px | 500 |
| `text-h5` | 20px / 24px | 500 |
| `text-h6` | 18px / 22px | 500 |
| `text-subtitle1` | 16px / 20px | 500 |
| `text-subtitle2` | 14px / 18px | 500 |
| `text-body1` | 16px / 20px | 400 |
| `text-body2` | 14px / 18px | 400 |
| `text-caption` | 12px / 16px | 400 |

### Radius & shadow

- `--radius`: `0.625rem` → `0.75rem`.
- New `--shadow-card`: soft, subtle elevation (e.g.
  `0 1px 2px rgba(27,27,31,0.04), 0 4px 12px rgba(27,27,31,0.04)`), used by `Card`
  instead of relying solely on a border.

## Component changes (`frontend/src/components/ui/*`)

- **Card**: `rounded-lg` → `rounded-xl`, add `shadow-[var(--shadow-card)]`, lighten
  border to `border-border/60`.
- **Badge**: add `success`, `warning`, `info` variants (same cva pattern as
  existing `default/secondary/destructive/outline`), using the `-lighter`
  background + `-dark` text pairing for each, matching SaasAble's soft badge style.
  Existing call sites (`DomainsPage`, `BillingPage`, `ApiKeysPage`, `LinksPage`,
  `LinkDetailPage`) get their `variant` props updated to use the new semantic
  variants where appropriate (e.g. verified domain → `success`, pending →
  `warning`).
- **Button, Input, Label, Dialog**: no structural changes; they inherit the new
  tokens automatically since they reference CSS variables, not literal colors.
- New **DropdownMenu** primitive (`frontend/src/components/ui/dropdown-menu.tsx`)
  wrapping `@radix-ui/react-dropdown-menu` (new dependency), shadcn-style, used by
  the Topbar user menu.

## Routing & information architecture (`frontend/src/App.tsx`)

```
/                    → LandingPage (public)
/login               → LoginPage (public, restyled)
/register            → RegisterPage (public, restyled)
/unlock/:code         → UnlockPage (public, light restyle)
[ProtectedRoute]
  [AppShell]
    /dashboard        → DashboardPage (new)
    /links            → LinksPage (moved from "/")
    /links/:code      → LinkDetailPage (unchanged path)
    /analytics        → AnalyticsPage (restyled)
    /domains          → DomainsPage (restyled)
    /api-keys         → ApiKeysPage (restyled)
    /billing          → BillingPage (restyled)
*                    → redirect to "/"
```

- `LoginPage`/`RegisterPage` navigate to `/dashboard` on success instead of `/`.
- `ProtectedRoute` is unchanged (still redirects unauthenticated → `/login`).
- No index route inside `AppShell` — every protected page has an explicit path.

## App shell (`frontend/src/layouts/AppShell.tsx` — rewritten)

Split into `Sidebar` + `Topbar` + content outlet.

**Sidebar** (`frontend/src/components/layout/Sidebar.tsx`):
- Logo ("Snapl" wordmark + icon), collapse toggle button (chevron), collapsed
  state persisted to `localStorage` (`snapl-sidebar-collapsed`), icon-only when
  collapsed with tooltips on hover.
- Two nav groups:
  - **Overview**: Dashboard (`LayoutDashboard`), Links (`Link2`), Analytics
    (`BarChart3`)
  - **Organization**: Domains (`Globe`), API Keys (`KeyRound`), Billing
    (`CreditCard`)
- Active route: left accent bar + `bg-primary-lighter text-primary` styling via
  `NavLink`'s `isActive`.
- On mobile (`<lg`), sidebar becomes an off-canvas drawer toggled from the Topbar.

**Topbar** (`frontend/src/components/layout/Topbar.tsx`):
- Left: mobile menu toggle (hidden ≥lg).
- Center-left: a real search input — on submit, `navigate(`/links?search=${q}`)`.
  `LinksPage` reads `useSearchParams().get("search")` as the initial value of its
  existing `search` state (one-line addition, reuses the existing
  `listUrls({ search })` call, no backend change).
- Right: theme toggle button, user avatar dropdown (initials avatar computed from
  `user.email`, shows email + role, "Log out" action calling existing
  `logout()`). No notifications bell, no settings link.

## Dashboard page (`frontend/src/pages/DashboardPage.tsx` — new)

All data from existing endpoints — `getOrgSummary(days)` and `getUsageSummary()`
(already used by `AnalyticsPage`/`BillingPage`), plus `listUrls()` for "recent
links".

- **Period tabs** (Daily / Monthly / Yearly) map to `getOrgSummary(7)` / `(30)` /
  `(365)`. Changing tabs re-triggers the query (new `useQuery` key including the
  selected range).
- **KPI row** (4 cards): Total clicks (`totalClicks`), Total links (`totalLinks`),
  Clicks in selected period (`sum(series.map(p => p.clicks))`), Best link
  (`topLinks[0]?.clicks ?? 0`, subtitle shows its short code).
- **Chart card**: reuse/extend `ClicksLineChart` as an area chart (switch
  `<Line>` to `<Area>` with gradient fill, matching SaasAble's filled line look);
  same `series` data, driven by the selected period.
- **Two-column widgets**:
  - "Top performing links" — `topLinks`, reusing `BreakdownList`-style rows
    (label + bar + count), each linking to `/links/:code`.
  - "Recent links" — latest 5 from `listUrls({ page: 0, size: 5 })`, showing
    short code, destination (truncated), created date.
- **Plan usage card**: reuses `getUsageSummary()` (same data as `BillingPage`),
  compact version of the existing `UsageMeter` bars, with a "Manage plan" link to
  `/billing`.
- Loading/empty states: skeleton/placeholder text consistent with existing pages
  (`"Loading…"`, `"No clicks yet."` etc.) — no new empty-state illustrations.

## Existing pages — restyle only

`LinksPage`, `LinkDetailPage`, `AnalyticsPage`, `DomainsPage`, `ApiKeysPage`,
`BillingPage`: no logic/data-fetching changes. Restyled via:
- New `Card`/`Badge` styling (automatic via token/component changes above).
- Headings switched from ad hoc `text-2xl font-semibold` to `text-h3` (or `h4`
  where nested) for consistency.
- `LinksPage` table restyled (spacing, muted header background using new
  `--muted`, row hover state) but same columns/behavior; add the `search` query
  param read described above.
- Status badges updated to semantic variants (`success`/`warning`/`destructive`)
  per the Badge section above.

## Landing page (`frontend/src/pages/LandingPage.tsx` — new)

Public, rendered outside `AppShell`. Uses `useAuth()` to swap the nav CTA to
"Go to Dashboard" (→ `/dashboard`) when `user` is present, otherwise "Sign in" /
"Get started".

Sections, top to bottom:
1. **Nav**: sticky, Snapl logo, links to in-page anchors (`#features`,
   `#pricing`), theme toggle, auth CTAs.
2. **Hero**: headline + subheadline + primary CTA (`/register`) + secondary CTA
   (scroll to `#features`). Visual: a new `DashboardPreviewMockup` component
   (`frontend/src/components/marketing/DashboardPreviewMockup.tsx`) — pure
   HTML/CSS/SVG built from the same tokens (mini stat tiles, a decorative chart
   shape, a couple of list rows), not a real screenshot asset and not a fake
   working shortener, since anonymous shortening isn't a public API. Shared
   between the landing page hero and the auth split-screen panel.
3. **Feature grid** (6 cards, real capabilities only): Custom domains,
   Real-time click analytics, Scoped API keys, Team roles (Owner/Admin/Member),
   Password-protected & scheduled links, Rate-limited & hardened at scale.
4. **How it works** (3 steps): Create a link → Share it → Track clicks.
5. **Pricing** (3 static cards from `V13__billing.sql` seed data):
   - Free — $0/mo, 25 links, 1,000 clicks/mo, no custom domains, no API access
   - Pro — $29/mo, 1,000 links, 50,000 clicks/mo, 1 custom domain, API access
   - Business — $99/mo, unlimited links & clicks, 5 custom domains, API access
   Each card's CTA goes to `/register` (plan selection happens post-signup on
   the existing `BillingPage`, not on the landing page).
6. **Footer**: logo, links to `/login`/`/register`, copyright.

No trust/logo strip, no testimonials — nothing fabricated.

## Auth pages (`LoginPage`, `RegisterPage` — restyled)

Split-screen layout matching the SaasAble reference: form on the left (existing
fields/logic unchanged), right panel with a primary-color gradient background,
Snapl logo, one-line product tagline, and a static preview mockup of the
Dashboard page (reuse the same illustrative mockup component as the landing
page hero). Collapses to form-only (no right panel) below `md` breakpoint.

## Out of scope / explicitly excluded

- Backend changes of any kind.
- Live pricing API, notifications, RTL/language switch, settings/profile page.
- MUI migration.
- Automated visual regression tests (none exist today for the frontend);
  verification is manual via the dev server (see Verification below).

## Verification

No frontend automated test suite exists today (`package.json` has no test
script). Verification is manual:
- `npm run build` (runs `tsc -b && vite build`) must pass with no type errors.
- `npm run lint` (oxlint) must pass.
- Manually click through: landing page (logged out) → register → dashboard →
  each nav item → login/logout → theme toggle → sidebar collapse → mobile
  width (resize) for landing, dashboard, and one data page.
- Existing backend Java tests are untouched and out of scope for this change.
