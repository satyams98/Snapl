# Snapl UI/UX Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the URL-shortener frontend into a branded product ("Snapl") with a SaasAble-inspired visual system, a new public landing page, a new Dashboard home page, and restyled existing pages — with zero backend changes.

**Architecture:** All changes are confined to `frontend/src/`. Design tokens live in `index.css` (Tailwind v4 `@theme`). Layout is a new `Sidebar` + `Topbar` composed in `AppShell`. Every widget on every new page reads from an endpoint that already exists in `frontend/src/api/*` — nothing is fabricated.

**Tech Stack:** React 19, Vite, Tailwind CSS v4, Radix UI primitives (shadcn-style wrappers), TanStack Query v5, react-router-dom v7, recharts, lucide-react.

## Global Constraints

- Keep the existing Tailwind v4 + Radix/shadcn stack. Do not migrate to MUI.
- Make no backend changes. The pricing section on the landing page uses static
  copy sourced from the real seed data in
  `src/main/resources/db/migration/V13__billing.sql`: **Free** — $0/mo, 25
  links, 1,000 clicks/mo, 0 custom domains, no API access. **Pro** — $29/mo
  (2900 cents), 1,000 links, 50,000 clicks/mo, 1 custom domain, API access.
  **Business** — $99/mo (9900 cents), unlimited links, unlimited clicks, 5
  custom domains, API access.
- Product name is **"Snapl"**.
- Color tokens (light mode): primary `#606BDF`, success `#22892F`, warning
  `#AE6600`, info `#008394`, destructive `#DE3730`, background `#FBF8FF`,
  foreground `#1B1B1F`, border `#E4E1E6`, muted `#F5F2FA` /
  muted-foreground `#46464F`. Radius `0.75rem`. Font: Inter (Google Fonts).
- The only new npm dependency allowed is `@radix-ui/react-dropdown-menu`.
- No fabricated features or data: no notifications UI, no live pricing API
  call, no RTL/i18n, no settings/profile page, no customer logos or
  testimonials. Every number shown on screen must come from an existing
  `frontend/src/api/*` function.
- **No automated frontend test suite exists** (`frontend/package.json` has no
  test script) and adding one is out of scope. Every task is verified by: (1)
  `npm run build` (runs `tsc -b && vite build`) passing with no errors, (2)
  `npm run lint` (oxlint) passing, (3) a manual check in `npm run dev`
  described in the task's own verification step.
- `frontend/src/pages/UnlockPage.tsx` needs no code changes — it's a plain
  `Card`/`Button`/`Input` composition, so it automatically picks up the new
  design tokens from Task 2. Don't create a task for it.
- All commands below run with `frontend/` as the working directory unless
  stated otherwise.

---

### Task 1: Design tokens, typography, and fonts

**Files:**
- Modify: `frontend/src/index.css` (full rewrite)
- Modify: `frontend/index.html`

**Interfaces:**
- Produces: CSS custom properties `--primary`, `--primary-foreground`,
  `--primary-lighter`, `--primary-light`, `--primary-dark`, `--primary-darker`,
  `--secondary`, `--secondary-foreground`, `--muted`, `--muted-foreground`,
  `--accent`, `--accent-foreground`, `--destructive`,
  `--destructive-foreground`, `--destructive-lighter`, `--destructive-dark`,
  `--success`, `--success-foreground`, `--success-lighter`, `--success-dark`,
  `--warning`, `--warning-foreground`, `--warning-lighter`, `--warning-dark`,
  `--info`, `--info-foreground`, `--info-lighter`, `--info-dark`, `--border`,
  `--input`, `--ring`, `--radius`, `--shadow-card`. Produces Tailwind
  utilities `text-h1`…`text-h6`, `text-subtitle1`, `text-subtitle2`,
  `text-body1`, `text-body2`, `text-caption` (size + line-height only — pair
  with `font-medium`/`font-normal` explicitly in JSX, weight is NOT baked
  into these utilities). Produces `bg-success`, `bg-success-lighter`,
  `text-success-dark`, and the equivalent `warning`/`info`/`destructive`
  color utilities via the `@theme inline` block.
- Consumes: nothing (first task).

- [ ] **Step 1: Replace `frontend/src/index.css`**

```css
@import "tailwindcss";

:root {
  --background: #fbf8ff;
  --foreground: #1b1b1f;
  --card: #ffffff;
  --card-foreground: #1b1b1f;

  --primary: #606bdf;
  --primary-foreground: #ffffff;
  --primary-lighter: #e0e0ff;
  --primary-light: #bdc2ff;
  --primary-dark: #3944b8;
  --primary-darker: #000668;

  --secondary: #efedf4;
  --secondary-foreground: #1b1b1f;

  --muted: #f5f2fa;
  --muted-foreground: #46464f;

  --accent: #efedf4;
  --accent-foreground: #1b1b1f;

  --destructive: #de3730;
  --destructive-foreground: #ffffff;
  --destructive-lighter: #ffedea;
  --destructive-dark: #ba1a1a;

  --success: #22892f;
  --success-foreground: #ffffff;
  --success-lighter: #c8ffc0;
  --success-dark: #006e1c;

  --warning: #ae6600;
  --warning-foreground: #ffffff;
  --warning-lighter: #ffeee1;
  --warning-dark: #8b5000;

  --info: #008394;
  --info-foreground: #ffffff;
  --info-lighter: #d4f7ff;
  --info-dark: #006876;

  --border: #e4e1e6;
  --input: #e4e1e6;
  --ring: #606bdf;

  --radius: 0.75rem;
  --shadow-card: 0 1px 2px rgba(27, 27, 31, 0.04), 0 4px 12px rgba(27, 27, 31, 0.04);
}

:root[data-theme="dark"] {
  --background: #171a31;
  --foreground: #f5f2fa;
  --card: #1f2238;
  --card-foreground: #f5f2fa;

  --primary: #606bdf;
  --primary-foreground: #ffffff;
  --primary-lighter: rgba(96, 107, 223, 0.24);
  --primary-light: rgba(96, 107, 223, 0.4);
  --primary-dark: #bdc2ff;
  --primary-darker: #e0e0ff;

  --secondary: #232336;
  --secondary-foreground: #f5f2fa;

  --muted: #232336;
  --muted-foreground: #c3c4e4;

  --accent: #232336;
  --accent-foreground: #f5f2fa;

  --destructive: #ff6b61;
  --destructive-foreground: #171a31;
  --destructive-lighter: rgba(222, 55, 48, 0.18);
  --destructive-dark: #ffdad6;

  --success: #57d16a;
  --success-foreground: #171a31;
  --success-lighter: rgba(34, 137, 47, 0.18);
  --success-dark: #c8ffc0;

  --warning: #e2a33d;
  --warning-foreground: #171a31;
  --warning-lighter: rgba(174, 102, 0, 0.18);
  --warning-dark: #ffdcbe;

  --info: #22b8cc;
  --info-foreground: #171a31;
  --info-lighter: rgba(0, 131, 148, 0.18);
  --info-dark: #a1efff;

  --border: rgba(255, 255, 255, 0.08);
  --input: rgba(255, 255, 255, 0.12);
  --ring: #8890ea;

  --shadow-card: 0 1px 2px rgba(0, 0, 0, 0.24), 0 4px 12px rgba(0, 0, 0, 0.24);
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) {
    --background: #171a31;
    --foreground: #f5f2fa;
    --card: #1f2238;
    --card-foreground: #f5f2fa;

    --primary: #606bdf;
    --primary-foreground: #ffffff;
    --primary-lighter: rgba(96, 107, 223, 0.24);
    --primary-light: rgba(96, 107, 223, 0.4);
    --primary-dark: #bdc2ff;
    --primary-darker: #e0e0ff;

    --secondary: #232336;
    --secondary-foreground: #f5f2fa;

    --muted: #232336;
    --muted-foreground: #c3c4e4;

    --accent: #232336;
    --accent-foreground: #f5f2fa;

    --destructive: #ff6b61;
    --destructive-foreground: #171a31;
    --destructive-lighter: rgba(222, 55, 48, 0.18);
    --destructive-dark: #ffdad6;

    --success: #57d16a;
    --success-foreground: #171a31;
    --success-lighter: rgba(34, 137, 47, 0.18);
    --success-dark: #c8ffc0;

    --warning: #e2a33d;
    --warning-foreground: #171a31;
    --warning-lighter: rgba(174, 102, 0, 0.18);
    --warning-dark: #ffdcbe;

    --info: #22b8cc;
    --info-foreground: #171a31;
    --info-lighter: rgba(0, 131, 148, 0.18);
    --info-dark: #a1efff;

    --border: rgba(255, 255, 255, 0.08);
    --input: rgba(255, 255, 255, 0.12);
    --ring: #8890ea;

    --shadow-card: 0 1px 2px rgba(0, 0, 0, 0.24), 0 4px 12px rgba(0, 0, 0, 0.24);
  }
}

@theme inline {
  --color-background: var(--background);
  --color-foreground: var(--foreground);
  --color-card: var(--card);
  --color-card-foreground: var(--card-foreground);

  --color-primary: var(--primary);
  --color-primary-foreground: var(--primary-foreground);
  --color-primary-lighter: var(--primary-lighter);
  --color-primary-light: var(--primary-light);
  --color-primary-dark: var(--primary-dark);
  --color-primary-darker: var(--primary-darker);

  --color-secondary: var(--secondary);
  --color-secondary-foreground: var(--secondary-foreground);

  --color-muted: var(--muted);
  --color-muted-foreground: var(--muted-foreground);

  --color-accent: var(--accent);
  --color-accent-foreground: var(--accent-foreground);

  --color-destructive: var(--destructive);
  --color-destructive-foreground: var(--destructive-foreground);
  --color-destructive-lighter: var(--destructive-lighter);
  --color-destructive-dark: var(--destructive-dark);

  --color-success: var(--success);
  --color-success-foreground: var(--success-foreground);
  --color-success-lighter: var(--success-lighter);
  --color-success-dark: var(--success-dark);

  --color-warning: var(--warning);
  --color-warning-foreground: var(--warning-foreground);
  --color-warning-lighter: var(--warning-lighter);
  --color-warning-dark: var(--warning-dark);

  --color-info: var(--info);
  --color-info-foreground: var(--info-foreground);
  --color-info-lighter: var(--info-lighter);
  --color-info-dark: var(--info-dark);

  --color-border: var(--border);
  --color-input: var(--input);
  --color-ring: var(--ring);

  --radius-sm: calc(var(--radius) - 4px);
  --radius-md: calc(var(--radius) - 2px);
  --radius-lg: var(--radius);
  --radius-xl: calc(var(--radius) + 4px);

  --font-sans: "Inter", ui-sans-serif, system-ui, sans-serif;

  --text-h1: 2.5rem;
  --text-h1--line-height: 2.75rem;
  --text-h2: 2rem;
  --text-h2--line-height: 2.25rem;
  --text-h3: 1.75rem;
  --text-h3--line-height: 2rem;
  --text-h4: 1.5rem;
  --text-h4--line-height: 1.75rem;
  --text-h5: 1.25rem;
  --text-h5--line-height: 1.5rem;
  --text-h6: 1.125rem;
  --text-h6--line-height: 1.375rem;
  --text-subtitle1: 1rem;
  --text-subtitle1--line-height: 1.25rem;
  --text-subtitle2: 0.875rem;
  --text-subtitle2--line-height: 1.125rem;
  --text-body1: 1rem;
  --text-body1--line-height: 1.25rem;
  --text-body2: 0.875rem;
  --text-body2--line-height: 1.125rem;
  --text-caption: 0.75rem;
  --text-caption--line-height: 1rem;
}

body {
  @apply bg-background text-foreground antialiased;
  margin: 0;
  font-family: var(--font-sans);
}

#root {
  min-height: 100svh;
}
```

- [ ] **Step 2: Add the Inter font and update the page title in `frontend/index.html`**

Replace the file with:

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap"
      rel="stylesheet"
    />
    <title>Snapl</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 3: Build**

Run (from `frontend/`): `npm run build`
Expected: succeeds with no TypeScript or build errors.

- [ ] **Step 4: Manual check**

Run: `npm run dev`, open the app in a browser. Log in with an existing test
account. Confirm: the page background is an off-white/lavender tint (not
pure white), primary buttons are indigo (`#606BDF`), and text renders in
Inter (check via devtools computed font-family).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/index.css frontend/index.html
git commit -m "feat(ui): add SaasAble-inspired design tokens and Inter font"
```

---

### Task 2: Restyle Card and Badge primitives

**Files:**
- Modify: `frontend/src/components/ui/card.tsx` (full rewrite)
- Modify: `frontend/src/components/ui/badge.tsx` (full rewrite)

**Interfaces:**
- Consumes: `--shadow-card`, `--color-success*`, `--color-warning*`,
  `--color-info*`, `--color-destructive*` tokens from Task 1.
- Produces: `Card`/`CardHeader`/`CardTitle`/`CardDescription`/`CardContent`/
  `CardFooter` (same exports and props as before — no call-site changes
  needed anywhere in the app). `Badge` gains three new `variant` values:
  `"success"`, `"warning"`, `"info"`, in addition to the existing
  `"default" | "secondary" | "destructive" | "outline"`.

- [ ] **Step 1: Replace `frontend/src/components/ui/card.tsx`**

```tsx
import * as React from "react";
import { cn } from "@/lib/utils";

function Card({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      className={cn(
        "rounded-xl border border-border/60 bg-card text-card-foreground shadow-[var(--shadow-card)]",
        className,
      )}
      {...props}
    />
  );
}

function CardHeader({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex flex-col space-y-1.5 p-6", className)} {...props} />;
}

function CardTitle({ className, ...props }: React.ComponentProps<"h3">) {
  return <h3 className={cn("text-h6 font-medium leading-none tracking-tight", className)} {...props} />;
}

function CardDescription({ className, ...props }: React.ComponentProps<"p">) {
  return <p className={cn("text-body2 text-muted-foreground", className)} {...props} />;
}

function CardContent({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("p-6 pt-0", className)} {...props} />;
}

function CardFooter({ className, ...props }: React.ComponentProps<"div">) {
  return <div className={cn("flex items-center p-6 pt-0", className)} {...props} />;
}

export { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter };
```

- [ ] **Step 2: Replace `frontend/src/components/ui/badge.tsx`**

```tsx
import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-md border px-2 py-0.5 text-caption font-medium transition-colors",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        destructive: "border-transparent bg-destructive-lighter text-destructive-dark",
        success: "border-transparent bg-success-lighter text-success-dark",
        warning: "border-transparent bg-warning-lighter text-warning-dark",
        info: "border-transparent bg-info-lighter text-info-dark",
        outline: "text-foreground",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  },
);

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
```

- [ ] **Step 3: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 4: Manual check**

Run `npm run dev`. Visit `/` (Links list), `/billing`, `/domains`,
`/api-keys` and `/unlock/<any-code>` (this one will show a "not found"
style error after submit, but the card itself should render). Confirm cards
now have larger rounded corners and a soft shadow instead of a hard border,
and existing badges (tags, "Disabled", "Revoked", plan status) render with
their new soft-colored style automatically — no other file changed.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/ui/card.tsx frontend/src/components/ui/badge.tsx
git commit -m "feat(ui): restyle Card and Badge with new tokens and semantic variants"
```

---

### Task 3: New AppShell — Sidebar, Topbar, theme toggle, user menu, link search

**Files:**
- Create: `frontend/src/hooks/useTheme.ts`
- Create: `frontend/src/components/ui/dropdown-menu.tsx`
- Create: `frontend/src/components/layout/Sidebar.tsx`
- Create: `frontend/src/components/layout/Topbar.tsx`
- Modify: `frontend/src/layouts/AppShell.tsx` (full rewrite)
- Modify: `frontend/src/pages/LinksPage.tsx:1-19` (read initial search from URL)
- Modify: `frontend/package.json` (new dependency)

**Interfaces:**
- Consumes: `useAuth()` from `frontend/src/context/AuthContext.tsx` (existing:
  `{ user: AuthUser | null, logout: () => Promise<void> }`). Design tokens
  from Task 1. `Card`/`Badge` styling from Task 2 is inherited automatically,
  not directly used here.
- Produces: `useTheme()` hook returning
  `{ theme: "light" | "dark", setTheme: (t) => void, toggle: () => void }`,
  persisted to `localStorage["snapl-theme"]` and applied via
  `document.documentElement.dataset.theme`. This is imported directly by
  Task 5 (Landing page) and Task 8 (auth pages) — do not rename its exports.
  `DropdownMenu`, `DropdownMenuTrigger`, `DropdownMenuContent`,
  `DropdownMenuItem`, `DropdownMenuLabel`, `DropdownMenuSeparator` from the
  new `dropdown-menu.tsx` primitive. `Sidebar` component with props
  `{ collapsed: boolean; onToggleCollapsed: () => void; onNavigate?: () => void }`.
  `Topbar` component with props `{ onOpenMobileNav: () => void }`.

- [ ] **Step 1: Install the new dependency**

Run (from `frontend/`): `npm install @radix-ui/react-dropdown-menu`
Expected: adds one entry to `dependencies` in `frontend/package.json` and
updates the lockfile.

- [ ] **Step 2: Create `frontend/src/hooks/useTheme.ts`**

```ts
import { useCallback, useEffect, useState } from "react";

export type Theme = "light" | "dark";

const STORAGE_KEY = "snapl-theme";

function getStoredTheme(): Theme | null {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === "light" || stored === "dark" ? stored : null;
}

function getSystemTheme(): Theme {
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(() => getStoredTheme() ?? getSystemTheme());

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, [theme]);

  const setTheme = useCallback((next: Theme) => {
    localStorage.setItem(STORAGE_KEY, next);
    setThemeState(next);
  }, []);

  const toggle = useCallback(() => {
    setTheme(theme === "dark" ? "light" : "dark");
  }, [theme, setTheme]);

  return { theme, setTheme, toggle };
}
```

- [ ] **Step 3: Create `frontend/src/components/ui/dropdown-menu.tsx`**

```tsx
import * as React from "react";
import * as DropdownMenuPrimitive from "@radix-ui/react-dropdown-menu";
import { cn } from "@/lib/utils";

const DropdownMenu = DropdownMenuPrimitive.Root;
const DropdownMenuTrigger = DropdownMenuPrimitive.Trigger;

function DropdownMenuContent({
  className,
  sideOffset = 8,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Content>) {
  return (
    <DropdownMenuPrimitive.Portal>
      <DropdownMenuPrimitive.Content
        sideOffset={sideOffset}
        className={cn(
          "z-50 min-w-[220px] overflow-hidden rounded-lg border border-border/60 bg-card p-1.5 text-card-foreground shadow-[var(--shadow-card)]",
          className,
        )}
        {...props}
      />
    </DropdownMenuPrimitive.Portal>
  );
}

function DropdownMenuItem({ className, ...props }: React.ComponentProps<typeof DropdownMenuPrimitive.Item>) {
  return (
    <DropdownMenuPrimitive.Item
      className={cn(
        "flex cursor-pointer select-none items-center gap-2 rounded-md px-2.5 py-2 text-body2 outline-none transition-colors",
        "focus:bg-accent focus:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
        className,
      )}
      {...props}
    />
  );
}

function DropdownMenuLabel({ className, ...props }: React.ComponentProps<typeof DropdownMenuPrimitive.Label>) {
  return (
    <DropdownMenuPrimitive.Label className={cn("px-2.5 py-1.5 text-caption text-muted-foreground", className)} {...props} />
  );
}

function DropdownMenuSeparator({
  className,
  ...props
}: React.ComponentProps<typeof DropdownMenuPrimitive.Separator>) {
  return <DropdownMenuPrimitive.Separator className={cn("my-1 h-px bg-border", className)} {...props} />;
}

export {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
};
```

- [ ] **Step 4: Create `frontend/src/components/layout/Sidebar.tsx`**

```tsx
import { NavLink } from "react-router-dom";
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
import { cn } from "@/lib/utils";

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const NAV_GROUPS: NavGroup[] = [
  {
    label: "Overview",
    items: [
      { to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
      { to: "/links", label: "Links", icon: Link2 },
      { to: "/analytics", label: "Analytics", icon: BarChart3 },
    ],
  },
  {
    label: "Organization",
    items: [
      { to: "/domains", label: "Domains", icon: Globe },
      { to: "/api-keys", label: "API Keys", icon: KeyRound },
      { to: "/billing", label: "Billing", icon: CreditCard },
    ],
  },
];

export function Sidebar({
  collapsed,
  onToggleCollapsed,
  onNavigate,
}: {
  collapsed: boolean;
  onToggleCollapsed: () => void;
  onNavigate?: () => void;
}) {
  return (
    <div className="flex h-full flex-col bg-card">
      <div className={cn("flex h-16 items-center gap-2 border-b border-border/60 px-4", collapsed && "justify-center px-2")}>
        <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <Zap className="size-4" />
        </div>
        {!collapsed && <span className="text-h6 font-medium">Snapl</span>}
        <button
          type="button"
          onClick={onToggleCollapsed}
          className="ml-auto hidden size-8 shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground lg:flex"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
        >
          {collapsed ? <PanelLeftOpen className="size-4" /> : <PanelLeftClose className="size-4" />}
        </button>
      </div>

      <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-4">
        {NAV_GROUPS.map((group) => (
          <div key={group.label}>
            {!collapsed && (
              <p className="mb-2 px-3 text-caption font-medium uppercase tracking-wide text-muted-foreground">
                {group.label}
              </p>
            )}
            <div className="space-y-1">
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-body2 font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground",
                      collapsed && "justify-center px-0",
                      isActive && "bg-primary-lighter text-primary hover:bg-primary-lighter hover:text-primary",
                    )
                  }
                  title={collapsed ? item.label : undefined}
                >
                  <item.icon className="size-4 shrink-0" />
                  {!collapsed && <span>{item.label}</span>}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>
    </div>
  );
}
```

- [ ] **Step 5: Create `frontend/src/components/layout/Topbar.tsx`**

```tsx
import { useState, type FormEvent } from "react";
import { Menu, MoonStar, Search, SunMedium } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useTheme } from "@/hooks/useTheme";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function Topbar({ onOpenMobileNav }: { onOpenMobileNav: () => void }) {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");

  function handleSearchSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = search.trim();
    navigate(trimmed ? `/links?search=${encodeURIComponent(trimmed)}` : "/links");
  }

  const initials = (user?.email ?? "?").slice(0, 2).toUpperCase();

  return (
    <header className="flex h-16 items-center gap-3 border-b border-border/60 bg-card px-4 lg:px-6">
      <button
        type="button"
        onClick={onOpenMobileNav}
        className="flex size-9 shrink-0 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground lg:hidden"
        aria-label="Open navigation"
      >
        <Menu className="size-5" />
      </button>

      <form onSubmit={handleSearchSubmit} className="hidden max-w-xs flex-1 sm:block">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search links…"
            className="pl-9"
          />
        </div>
      </form>

      <div className="ml-auto flex items-center gap-2">
        <Button
          type="button"
          variant="ghost"
          size="icon"
          onClick={toggle}
          aria-label={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
        >
          {theme === "dark" ? <SunMedium className="size-4" /> : <MoonStar className="size-4" />}
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="flex size-9 items-center justify-center rounded-full bg-primary-lighter text-body2 font-medium text-primary"
            >
              {initials}
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel className="flex flex-col gap-0.5">
              <span className="text-body2 font-medium text-foreground">{user?.email}</span>
              <span className="uppercase">{user?.role}</span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => void logout()}>Log out</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
```

- [ ] **Step 6: Replace `frontend/src/layouts/AppShell.tsx`**

```tsx
import { useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "@/components/layout/Sidebar";
import { Topbar } from "@/components/layout/Topbar";
import { cn } from "@/lib/utils";

const COLLAPSE_STORAGE_KEY = "snapl-sidebar-collapsed";

function getStoredCollapsed(): boolean {
  return localStorage.getItem(COLLAPSE_STORAGE_KEY) === "true";
}

export function AppShell() {
  const [collapsed, setCollapsed] = useState(getStoredCollapsed);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  function toggleCollapsed() {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(COLLAPSE_STORAGE_KEY, String(next));
      return next;
    });
  }

  return (
    <div className="flex min-h-svh bg-background">
      <aside
        className={cn(
          "hidden shrink-0 border-r border-border/60 transition-[width] duration-200 lg:block",
          collapsed ? "w-[72px]" : "w-64",
        )}
      >
        <div className="fixed h-svh" style={{ width: collapsed ? 72 : 256 }}>
          <Sidebar collapsed={collapsed} onToggleCollapsed={toggleCollapsed} />
        </div>
      </aside>

      {mobileNavOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-black/40" onClick={() => setMobileNavOpen(false)} />
          <div className="absolute inset-y-0 left-0 w-64 bg-card shadow-[var(--shadow-card)]">
            <Sidebar
              collapsed={false}
              onToggleCollapsed={() => setMobileNavOpen(false)}
              onNavigate={() => setMobileNavOpen(false)}
            />
          </div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenMobileNav={() => setMobileNavOpen(true)} />
        <main className="flex-1 p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
```

- [ ] **Step 7: Wire the search param into `frontend/src/pages/LinksPage.tsx`**

Change the import on line 3 from:

```tsx
import { Link } from "react-router-dom";
```

to:

```tsx
import { Link, useSearchParams } from "react-router-dom";
```

Change the top of the component (originally lines 13-14) from:

```tsx
export default function LinksPage() {
  const [search, setSearch] = useState("");
```

to:

```tsx
export default function LinksPage() {
  const [searchParams] = useSearchParams();
  const [search, setSearch] = useState(() => searchParams.get("search") ?? "");
```

- [ ] **Step 8: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 9: Manual check**

Run `npm run dev`, log in. Confirm: sidebar shows two grouped sections with
icons; clicking the collapse chevron shrinks it to icon-only and persists
across a page reload; on a narrow window (`<1024px`) the sidebar disappears
and a hamburger button in the topbar opens it as an overlay; the sun/moon
button toggles dark mode and the choice survives a reload; the avatar in the
top-right opens a dropdown showing your email/role and a working "Log out".
Type something in the topbar search box and press Enter — the browser
navigates to `/links?search=...` and the Links table filters accordingly.
**Note:** the "Dashboard" nav item won't resolve correctly until Task 4, and
"Links" won't resolve at its new `/links` path until Task 5 — that's
expected at this point; verify Analytics/Domains/API Keys/Billing instead.

- [ ] **Step 10: Commit**

```bash
git add frontend/src/hooks/useTheme.ts frontend/src/components/ui/dropdown-menu.tsx \
  frontend/src/components/layout/Sidebar.tsx frontend/src/components/layout/Topbar.tsx \
  frontend/src/layouts/AppShell.tsx frontend/src/pages/LinksPage.tsx \
  frontend/package.json frontend/package-lock.json
git commit -m "feat(ui): rebuild AppShell with sidebar, topbar, theme toggle, and user menu"
```

---

### Task 4: Area chart + Dashboard page

**Files:**
- Modify: `frontend/src/components/analytics/ClicksLineChart.tsx` (full rewrite)
- Create: `frontend/src/pages/DashboardPage.tsx`
- Modify: `frontend/src/App.tsx:31` (add one route, additive)

**Interfaces:**
- Consumes: `getOrgSummary(days?: number): Promise<AnalyticsSummary>` and
  `getUsageSummary(): Promise<UsageSummary>` (existing, from
  `frontend/src/api/analytics.ts` and `frontend/src/api/billing.ts`),
  `listUrls(params: ListUrlsParams): Promise<UrlListResponse>` (existing,
  from `frontend/src/api/urls.ts`). `Card`/`CardHeader`/`CardTitle`/
  `CardDescription`/`CardContent` from Task 2. `Button` (existing).
- Produces: `ClicksLineChart` keeps its existing signature
  `({ data: DailyClickPoint[] }) => JSX.Element` — Task 6 relies on this
  staying unchanged since `AnalyticsPage` and `LinkDetailPage` already import
  it. `DashboardPage` default export, mounted at `/dashboard`.

- [ ] **Step 1: Replace `frontend/src/components/analytics/ClicksLineChart.tsx`**

```tsx
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { DailyClickPoint } from "@/api/analytics";

export function ClicksLineChart({ data }: { data: DailyClickPoint[] }) {
  return (
    <ResponsiveContainer width="100%" height={280}>
      <AreaChart data={data}>
        <defs>
          <linearGradient id="clicksFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--color-primary)" stopOpacity={0.35} />
            <stop offset="100%" stopColor="var(--color-primary)" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" className="stroke-border" vertical={false} />
        <XAxis dataKey="day" tickFormatter={(value: string) => value.slice(5)} tick={{ fontSize: 12 }} />
        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} width={32} />
        <Tooltip />
        <Area type="monotone" dataKey="clicks" stroke="var(--color-primary)" strokeWidth={2} fill="url(#clicksFill)" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
```

- [ ] **Step 2: Create `frontend/src/pages/DashboardPage.tsx`**

```tsx
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ClicksLineChart } from "@/components/analytics/ClicksLineChart";
import { getOrgSummary } from "@/api/analytics";
import { getUsageSummary } from "@/api/billing";
import { listUrls } from "@/api/urls";

type Period = "daily" | "monthly" | "yearly";

const PERIOD_DAYS: Record<Period, number> = { daily: 7, monthly: 30, yearly: 365 };
const PERIOD_LABELS: Record<Period, string> = { daily: "Daily", monthly: "Monthly", yearly: "Yearly" };

function formatLimit(value: number): string {
  return value < 0 ? "Unlimited" : String(value);
}

export default function DashboardPage() {
  const [period, setPeriod] = useState<Period>("monthly");

  const summaryQuery = useQuery({
    queryKey: ["analytics", "summary", PERIOD_DAYS[period]],
    queryFn: () => getOrgSummary(PERIOD_DAYS[period]),
  });
  const usageQuery = useQuery({ queryKey: ["billing", "summary"], queryFn: getUsageSummary });
  const recentLinksQuery = useQuery({
    queryKey: ["urls", "recent"],
    queryFn: () => listUrls({ page: 0, size: 5 }),
  });

  const summary = summaryQuery.data;
  const periodClicks = (summary?.series ?? []).reduce((sum, point) => sum + point.clicks, 0);
  const topLink = summary?.topLinks[0];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-h3 font-medium">Dashboard</h1>
        <p className="text-body2 text-muted-foreground">A live overview of your links and traffic.</p>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader>
            <CardDescription>Total clicks</CardDescription>
            <CardTitle className="text-h4">{summary?.totalClicks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Total links</CardDescription>
            <CardTitle className="text-h4">{summary?.totalLinks ?? "—"}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Clicks this period</CardDescription>
            <CardTitle className="text-h4">{summaryQuery.isLoading ? "—" : periodClicks}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Best performing link</CardDescription>
            <CardTitle className="text-h4">{topLink?.clicks ?? "—"}</CardTitle>
            {topLink && <p className="text-caption text-muted-foreground">{topLink.shortCode}</p>}
          </CardHeader>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle>Clicks over time</CardTitle>
            <CardDescription>Track how your links are performing.</CardDescription>
          </div>
          <div className="flex gap-1 rounded-md bg-muted p-1">
            {(Object.keys(PERIOD_LABELS) as Period[]).map((key) => (
              <button
                key={key}
                type="button"
                onClick={() => setPeriod(key)}
                className={`rounded-sm px-3 py-1 text-body2 font-medium transition-colors ${
                  period === key ? "bg-card text-foreground shadow-[var(--shadow-card)]" : "text-muted-foreground"
                }`}
              >
                {PERIOD_LABELS[key]}
              </button>
            ))}
          </div>
        </CardHeader>
        <CardContent>
          {summaryQuery.isLoading ? (
            <p className="text-body2 text-muted-foreground">Loading…</p>
          ) : (
            <ClicksLineChart data={summary?.series ?? []} />
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Top performing links</CardTitle>
          </CardHeader>
          <CardContent>
            {(summary?.topLinks ?? []).length === 0 && (
              <p className="text-body2 text-muted-foreground">No clicks yet.</p>
            )}
            <ul className="divide-y divide-border/60">
              {(summary?.topLinks ?? []).map((link) => (
                <li key={link.shortCode} className="flex items-center justify-between gap-3 py-2 text-body2">
                  <Link to={`/links/${link.shortCode}`} className="font-medium text-primary hover:underline">
                    {link.shortCode}
                  </Link>
                  <span className="shrink-0 text-muted-foreground">{link.clicks} clicks</span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Recent links</CardTitle>
          </CardHeader>
          <CardContent>
            {(recentLinksQuery.data?.items ?? []).length === 0 && (
              <p className="text-body2 text-muted-foreground">No links yet.</p>
            )}
            <ul className="divide-y divide-border/60">
              {(recentLinksQuery.data?.items ?? []).map((item) => (
                <li key={item.shortCode} className="py-2 text-body2">
                  <Link to={`/links/${item.shortCode}`} className="font-medium text-primary hover:underline">
                    {item.shortCode}
                  </Link>
                  <p className="truncate text-caption text-muted-foreground">{item.longUrl}</p>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Plan usage</CardTitle>
            <CardDescription>{usageQuery.data?.planName ?? "—"} plan</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {usageQuery.data && (
              <>
                <div className="space-y-1">
                  <div className="flex justify-between text-body2">
                    <span>Active links</span>
                    <span className="text-muted-foreground">
                      {usageQuery.data.activeLinks} / {formatLimit(usageQuery.data.maxLinks)}
                    </span>
                  </div>
                  {usageQuery.data.maxLinks >= 0 && (
                    <div className="h-2 rounded-full bg-muted">
                      <div
                        className="h-2 rounded-full bg-primary"
                        style={{
                          width: `${Math.min(100, Math.round((usageQuery.data.activeLinks / Math.max(usageQuery.data.maxLinks, 1)) * 100))}%`,
                        }}
                      />
                    </div>
                  )}
                </div>
                <Button asChild size="sm" variant="outline" className="w-full">
                  <Link to="/billing">Manage plan</Link>
                </Button>
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Add the `/dashboard` route in `frontend/src/App.tsx`**

Add the import alongside the other page imports:

```tsx
import DashboardPage from "@/pages/DashboardPage";
```

Add the route as the first child inside the existing `<Route element={<AppShell />}>` block, immediately before `<Route index element={<LinksPage />} />`:

```tsx
                <Route path="dashboard" element={<DashboardPage />} />
```

- [ ] **Step 4: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 5: Manual check**

Run `npm run dev`, log in, navigate directly to `/dashboard` (or click
"Dashboard" in the sidebar). Confirm: 4 KPI cards show real numbers, the
Daily/Monthly/Yearly tabs each re-fetch and redraw the area chart, "Top
performing links" and "Recent links" show real data (or their empty-state
text if you have none), and "Plan usage" shows your real usage with a
working "Manage plan" link to `/billing`.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/analytics/ClicksLineChart.tsx frontend/src/pages/DashboardPage.tsx frontend/src/App.tsx
git commit -m "feat(dashboard): add Dashboard overview page with real KPIs and charts"
```

---

### Task 5: Routing cutover + Landing page

**Files:**
- Create: `frontend/src/components/marketing/DashboardPreviewMockup.tsx`
- Create: `frontend/src/pages/LandingPage.tsx`
- Modify: `frontend/src/App.tsx` (full rewrite)
- Modify: `frontend/src/pages/auth/LoginPage.tsx:24`
- Modify: `frontend/src/pages/auth/RegisterPage.tsx:26`

**Interfaces:**
- Consumes: `useTheme()` from Task 3. `useAuth()` (existing). `Card`/
  `CardHeader`/`CardTitle`/`CardDescription`/`CardContent`, `Button`
  (existing/Task 2).
- Produces: `DashboardPreviewMockup` (no props, pure presentational) —
  reused by Task 8. `LandingPage` default export mounted at `/`.

- [ ] **Step 1: Create `frontend/src/components/marketing/DashboardPreviewMockup.tsx`**

```tsx
export function DashboardPreviewMockup() {
  return (
    <div className="w-full max-w-md rounded-2xl border border-border/60 bg-card p-4 shadow-[var(--shadow-card)]">
      <div className="mb-4 flex items-center justify-between">
        <div className="h-3 w-24 rounded-full bg-muted" />
        <div className="flex gap-1.5">
          <div className="size-2.5 rounded-full bg-destructive-lighter" />
          <div className="size-2.5 rounded-full bg-warning-lighter" />
          <div className="size-2.5 rounded-full bg-success-lighter" />
        </div>
      </div>

      <div className="mb-4 grid grid-cols-3 gap-2">
        {["Clicks", "Links", "Domains"].map((label) => (
          <div key={label} className="rounded-lg bg-muted p-3">
            <p className="text-caption text-muted-foreground">{label}</p>
            <div className="mt-2 h-4 w-12 rounded bg-primary-lighter" />
          </div>
        ))}
      </div>

      <div className="mb-4 h-28 rounded-lg bg-muted p-3">
        <svg viewBox="0 0 200 60" className="h-full w-full" preserveAspectRatio="none">
          <polyline
            fill="none"
            stroke="var(--color-primary)"
            strokeWidth="2"
            points="0,45 25,35 50,40 75,20 100,28 125,15 150,22 175,8 200,14"
          />
        </svg>
      </div>

      <div className="space-y-2">
        {[1, 2, 3].map((row) => (
          <div key={row} className="flex items-center justify-between">
            <div className="h-2.5 w-20 rounded-full bg-muted" />
            <div className="h-2.5 w-10 rounded-full bg-muted" />
          </div>
        ))}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Create `frontend/src/pages/LandingPage.tsx`**

```tsx
import { Link } from "react-router-dom";
import { BarChart3, Globe, KeyRound, Lock, MoonStar, ShieldCheck, SunMedium, Users, Zap } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { useTheme } from "@/hooks/useTheme";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { DashboardPreviewMockup } from "@/components/marketing/DashboardPreviewMockup";

const FEATURES = [
  { icon: Globe, title: "Custom domains", description: "Brand every short link with a domain your customers already trust." },
  { icon: BarChart3, title: "Real-time click analytics", description: "Kafka-backed pipelines stream click events into dashboards you can trust." },
  { icon: KeyRound, title: "Scoped API keys", description: "Automate link creation with read/write scoped keys built for CI and scripts." },
  { icon: Users, title: "Team roles", description: "Invite your team with Owner, Admin, and Member roles across one organization." },
  { icon: Lock, title: "Password-protected & scheduled links", description: "Gate sensitive links with a password, or schedule them to go live later." },
  { icon: ShieldCheck, title: "Built to scale", description: "Redis-backed rate limiting and deduplication keep things fast under load." },
];

const PLANS = [
  {
    code: "FREE",
    name: "Free",
    price: "$0",
    description: "Get started with the essentials.",
    features: ["25 active links", "1,000 clicks / month", "No custom domains", "No API access"],
  },
  {
    code: "PRO",
    name: "Pro",
    price: "$29",
    description: "For growing teams that need more headroom.",
    features: ["1,000 active links", "50,000 clicks / month", "1 custom domain", "API access included"],
    highlighted: true,
  },
  {
    code: "BUSINESS",
    name: "Business",
    price: "$99",
    description: "Unlimited scale for serious link infrastructure.",
    features: ["Unlimited links", "Unlimited clicks", "5 custom domains", "API access included"],
  },
];

const STEPS = [
  { step: "1", title: "Create a link", description: "Shorten any URL, with an optional custom alias, password, or schedule." },
  { step: "2", title: "Share it", description: "Use your own branded domain so every click stays on-brand." },
  { step: "3", title: "Track it", description: "Watch clicks roll in on your dashboard, broken down by referrer and device." },
];

function ThemeToggleButton() {
  const { theme, toggle } = useTheme();
  return (
    <Button type="button" variant="ghost" size="icon" onClick={toggle} aria-label="Toggle theme">
      {theme === "dark" ? <SunMedium className="size-4" /> : <MoonStar className="size-4" />}
    </Button>
  );
}

export default function LandingPage() {
  const { user } = useAuth();

  return (
    <div className="min-h-svh bg-background">
      <header className="sticky top-0 z-30 border-b border-border/60 bg-card/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 lg:px-6">
          <Link to="/" className="flex items-center gap-2">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <Zap className="size-4" />
            </div>
            <span className="text-h6 font-medium">Snapl</span>
          </Link>
          <nav className="hidden items-center gap-6 text-body2 text-muted-foreground sm:flex">
            <a href="#features" className="hover:text-foreground">Features</a>
            <a href="#pricing" className="hover:text-foreground">Pricing</a>
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggleButton />
            {user ? (
              <Button asChild size="sm">
                <Link to="/dashboard">Go to Dashboard</Link>
              </Button>
            ) : (
              <>
                <Button asChild variant="ghost" size="sm">
                  <Link to="/login">Sign in</Link>
                </Button>
                <Button asChild size="sm">
                  <Link to="/register">Get started</Link>
                </Button>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="mx-auto flex max-w-6xl flex-col items-center gap-10 px-4 py-20 lg:flex-row lg:px-6">
        <div className="flex-1 space-y-6 text-center lg:text-left">
          <h1 className="text-h1 font-medium">Short links, built for teams that scale.</h1>
          <p className="text-body1 text-muted-foreground">
            Snapl gives your organization branded custom domains, real-time click analytics, and a
            scoped API — all behind role-based access for your whole team.
          </p>
          <div className="flex flex-col items-center gap-3 sm:flex-row lg:justify-start">
            <Button asChild size="lg">
              <Link to="/register">Start for free</Link>
            </Button>
            <Button asChild size="lg" variant="outline">
              <a href="#features">See what's included</a>
            </Button>
          </div>
        </div>
        <div className="flex flex-1 justify-center">
          <DashboardPreviewMockup />
        </div>
      </section>

      <section id="features" className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">Everything your team needs</h2>
          <p className="mt-2 text-body1 text-muted-foreground">Real infrastructure under the hood, not just a redirect.</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((feature) => (
            <Card key={feature.title}>
              <CardHeader>
                <div className="mb-2 flex size-10 items-center justify-center rounded-lg bg-primary-lighter text-primary">
                  <feature.icon className="size-5" />
                </div>
                <CardTitle>{feature.title}</CardTitle>
                <CardDescription>{feature.description}</CardDescription>
              </CardHeader>
            </Card>
          ))}
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">How it works</h2>
        </div>
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
          {STEPS.map((item) => (
            <div key={item.step} className="text-center">
              <div className="mx-auto mb-3 flex size-10 items-center justify-center rounded-full bg-primary text-primary-foreground">
                {item.step}
              </div>
              <h3 className="text-h6 font-medium">{item.title}</h3>
              <p className="mt-1 text-body2 text-muted-foreground">{item.description}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="pricing" className="mx-auto max-w-6xl px-4 py-16 lg:px-6">
        <div className="mb-10 text-center">
          <h2 className="text-h2 font-medium">Simple, transparent pricing</h2>
          <p className="mt-2 text-body1 text-muted-foreground">Upgrade or downgrade any time.</p>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {PLANS.map((plan) => (
            <Card
              key={plan.code}
              className={plan.highlighted ? "border-primary shadow-[var(--shadow-card)] ring-1 ring-primary" : undefined}
            >
              <CardHeader>
                <CardTitle>{plan.name}</CardTitle>
                <CardDescription>{plan.description}</CardDescription>
                <p className="pt-2">
                  <span className="text-h3 font-medium">{plan.price}</span>
                  <span className="text-body2 text-muted-foreground">/mo</span>
                </p>
              </CardHeader>
              <CardContent className="space-y-3">
                <ul className="space-y-2 text-body2 text-muted-foreground">
                  {plan.features.map((feature) => (
                    <li key={feature}>{feature}</li>
                  ))}
                </ul>
                <Button asChild className="w-full" variant={plan.highlighted ? "default" : "outline"}>
                  <Link to="/register">Get started</Link>
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <footer className="border-t border-border/60">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 px-4 py-8 text-body2 text-muted-foreground sm:flex-row lg:px-6">
          <span>© {new Date().getFullYear()} Snapl</span>
          <div className="flex gap-4">
            <Link to="/login" className="hover:text-foreground">Sign in</Link>
            <Link to="/register" className="hover:text-foreground">Create account</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
```

- [ ] **Step 3: Replace `frontend/src/App.tsx`**

```tsx
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { AppShell } from "@/layouts/AppShell";
import LandingPage from "@/pages/LandingPage";
import LoginPage from "@/pages/auth/LoginPage";
import RegisterPage from "@/pages/auth/RegisterPage";
import DashboardPage from "@/pages/DashboardPage";
import LinksPage from "@/pages/LinksPage";
import LinkDetailPage from "@/pages/LinkDetailPage";
import AnalyticsPage from "@/pages/AnalyticsPage";
import DomainsPage from "@/pages/DomainsPage";
import ApiKeysPage from "@/pages/ApiKeysPage";
import UnlockPage from "@/pages/UnlockPage";
import BillingPage from "@/pages/BillingPage";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/unlock/:code" element={<UnlockPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppShell />}>
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="links" element={<LinksPage />} />
                <Route path="links/:code" element={<LinkDetailPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="domains" element={<DomainsPage />} />
                <Route path="api-keys" element={<ApiKeysPage />} />
                <Route path="billing" element={<BillingPage />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
```

- [ ] **Step 4: Update the post-login redirect in `frontend/src/pages/auth/LoginPage.tsx`**

Change line 24 from:

```tsx
      navigate("/", { replace: true });
```

to:

```tsx
      navigate("/dashboard", { replace: true });
```

- [ ] **Step 5: Update the post-registration redirect in `frontend/src/pages/auth/RegisterPage.tsx`**

Change line 26 from:

```tsx
      navigate("/", { replace: true });
```

to:

```tsx
      navigate("/dashboard", { replace: true });
```

- [ ] **Step 6: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 7: Manual check**

Run `npm run dev`. Log out (or open an incognito window) and visit `/` —
confirm the new public landing page renders with working "Sign in"/"Get
started" links, the feature grid, the three pricing cards (Free/Pro/
Business with the correct numbers above), and the theme toggle in the nav.
Log in — visit `/` again and confirm the nav CTA now reads "Go to
Dashboard" and takes you to `/dashboard`. Confirm `/login` and `/register`
now redirect to `/dashboard` on success, and `/links` (not `/`) shows the
links table with all existing functionality (create/edit/search/pagination)
intact.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/marketing/DashboardPreviewMockup.tsx frontend/src/pages/LandingPage.tsx \
  frontend/src/App.tsx frontend/src/pages/auth/LoginPage.tsx frontend/src/pages/auth/RegisterPage.tsx
git commit -m "feat(landing): add public landing page and cut over routing to /dashboard + /links"
```

---

### Task 6: Restyle Links, Link Detail, and Analytics pages

**Files:**
- Modify: `frontend/src/pages/LinksPage.tsx`
- Modify: `frontend/src/pages/LinkDetailPage.tsx`
- Modify: `frontend/src/pages/AnalyticsPage.tsx`

**Interfaces:**
- Consumes: `text-h3`/`text-h4` typography utilities from Task 1, `success`
  Badge variant from Task 2. No data-fetching logic changes in this task.

- [ ] **Step 1: Update heading and status badge in `frontend/src/pages/LinksPage.tsx`**

Change:

```tsx
        <h1 className="text-2xl font-semibold">Links</h1>
```

to:

```tsx
        <h1 className="text-h3 font-medium">Links</h1>
```

Change:

```tsx
                    <Badge variant={item.disabled ? "destructive" : "default"}>
```

to:

```tsx
                    <Badge variant={item.disabled ? "destructive" : "success"}>
```

- [ ] **Step 2: Update heading and status badge in `frontend/src/pages/LinkDetailPage.tsx`**

Change:

```tsx
        <h1 className="text-2xl font-semibold">{code}</h1>
```

to:

```tsx
        <h1 className="text-h3 font-medium">{code}</h1>
```

Change:

```tsx
                  <Badge variant={link.disabled ? "destructive" : "default"}>
```

to:

```tsx
                  <Badge variant={link.disabled ? "destructive" : "success"}>
```

- [ ] **Step 3: Update heading and KPI card sizing in `frontend/src/pages/AnalyticsPage.tsx`**

Change:

```tsx
      <h1 className="text-2xl font-semibold">Analytics</h1>
```

to:

```tsx
      <h1 className="text-h3 font-medium">Analytics</h1>
```

Change both occurrences of:

```tsx
            <CardTitle className="text-3xl">{data?.totalClicks ?? "—"}</CardTitle>
```

and

```tsx
            <CardTitle className="text-3xl">{data?.totalLinks ?? "—"}</CardTitle>
```

to use `className="text-h4"` instead of `className="text-3xl"` (keep everything else on those lines unchanged).

- [ ] **Step 4: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 5: Manual check**

Run `npm run dev`. Visit `/links` — confirm the heading is smaller/medium
weight and active links show a green "Active" badge (disabled ones stay
red). Open a link's detail page (`/links/:code`) — same heading/badge
change. Visit `/analytics` — confirm the heading matches and the two KPI
numbers are sized consistently with the Dashboard page's KPI cards.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/LinksPage.tsx frontend/src/pages/LinkDetailPage.tsx frontend/src/pages/AnalyticsPage.tsx
git commit -m "style: restyle Links, Link Detail, and Analytics pages with new type scale"
```

---

### Task 7: Restyle Domains, API Keys, and Billing pages

**Files:**
- Modify: `frontend/src/pages/DomainsPage.tsx`
- Modify: `frontend/src/pages/ApiKeysPage.tsx`
- Modify: `frontend/src/pages/BillingPage.tsx`

**Interfaces:**
- Consumes: `text-h3` typography utility, `success`/`warning` Badge variants
  from Task 2. The backend `SubscriptionStatus` enum
  (`src/main/java/com/satyam/urlshortner/billing/SubscriptionStatus.java`)
  has exactly three values: `ACTIVE`, `PAST_DUE`, `CANCELED` — this is what
  `UsageSummary.subscriptionStatus` contains.

- [ ] **Step 1: Update heading and verification badge in `frontend/src/pages/DomainsPage.tsx`**

Change:

```tsx
          <h1 className="text-2xl font-semibold">Custom domains</h1>
```

to:

```tsx
          <h1 className="text-h3 font-medium">Custom domains</h1>
```

Change:

```tsx
              <Badge variant={item.verified ? "default" : "secondary"}>
```

to:

```tsx
              <Badge variant={item.verified ? "success" : "warning"}>
```

- [ ] **Step 2: Update heading in `frontend/src/pages/ApiKeysPage.tsx`**

Change:

```tsx
        <h1 className="text-2xl font-semibold">API keys</h1>
```

to:

```tsx
        <h1 className="text-h3 font-medium">API keys</h1>
```

- [ ] **Step 3: Update heading and subscription status badge in `frontend/src/pages/BillingPage.tsx`**

Change:

```tsx
        <h1 className="text-2xl font-semibold">Billing &amp; plan</h1>
```

to:

```tsx
        <h1 className="text-h3 font-medium">Billing &amp; plan</h1>
```

Change:

```tsx
            {usage && <Badge variant="secondary">{usage.subscriptionStatus}</Badge>}
```

to:

```tsx
            {usage && (
              <Badge variant={usage.subscriptionStatus === "ACTIVE" ? "success" : usage.subscriptionStatus === "PAST_DUE" ? "warning" : "destructive"}>
                {usage.subscriptionStatus}
              </Badge>
            )}
```

- [ ] **Step 4: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 5: Manual check**

Run `npm run dev`. Visit `/domains` — verified domains show a green badge,
unverified show amber. Visit `/api-keys` — confirm the heading change.
Visit `/billing` — confirm the heading change and that the plan status
badge is green when active (the default state for a freshly registered
org).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/DomainsPage.tsx frontend/src/pages/ApiKeysPage.tsx frontend/src/pages/BillingPage.tsx
git commit -m "style: restyle Domains, API Keys, and Billing pages with semantic status badges"
```

---

### Task 8: Restyle auth pages as split-screen

**Files:**
- Modify: `frontend/src/pages/auth/LoginPage.tsx` (full rewrite)
- Modify: `frontend/src/pages/auth/RegisterPage.tsx` (full rewrite)

**Interfaces:**
- Consumes: `DashboardPreviewMockup` from Task 5. `useAuth()` (existing).

- [ ] **Step 1: Replace `frontend/src/pages/auth/LoginPage.tsx`**

```tsx
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
```

- [ ] **Step 2: Replace `frontend/src/pages/auth/RegisterPage.tsx`**

```tsx
import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Zap } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { DashboardPreviewMockup } from "@/components/marketing/DashboardPreviewMockup";

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [organizationName, setOrganizationName] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsSubmitting(true);
    try {
      await register(email, password, name, organizationName);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Unable to create your account. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm space-y-6">
          <div className="space-y-1 text-center lg:text-left">
            <h1 className="text-h3 font-medium">Create your business account</h1>
            <p className="text-body2 text-muted-foreground">Sets you up as the Owner of a new organization.</p>
          </div>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="organizationName">Organization name</Label>
              <Input
                id="organizationName"
                required
                value={organizationName}
                onChange={(e) => setOrganizationName(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="name">Your name</Label>
              <Input id="name" required value={name} onChange={(e) => setName(e.target.value)} />
            </div>
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
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            {error && <p className="text-body2 text-destructive">{error}</p>}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Creating account…" : "Create account"}
            </Button>
          </form>
          <p className="text-center text-body2 text-muted-foreground lg:text-left">
            Already have an account?{" "}
            <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
              Sign in
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
          Join teams shipping branded links with real-time analytics and role-based access.
        </p>
        <DashboardPreviewMockup />
      </div>
    </div>
  );
}
```

- [ ] **Step 3: Build**

Run: `npm run build`
Expected: succeeds with no errors.

- [ ] **Step 4: Manual check**

Run `npm run dev`. Visit `/login` and `/register` at a wide viewport —
confirm a two-column split screen with the form on the left and a lavender
panel with the Snapl logo and the dashboard preview mockup on the right.
Resize below `1024px` — confirm the right panel disappears and only the
form remains, centered. Submit each form with valid data and confirm you
land on `/dashboard`.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/auth/LoginPage.tsx frontend/src/pages/auth/RegisterPage.tsx
git commit -m "style: restyle Login and Register as split-screen auth pages"
```

---

## Self-Review Notes

- **Spec coverage:** tokens/typography (Task 1) → Card/Badge (Task 2) →
  Sidebar/Topbar/theme/dropdown/search (Task 3) → area chart + Dashboard
  (Task 4) → routing cutover + Landing (Task 5) → Links/Detail/Analytics
  restyle (Task 6) → Domains/API Keys/Billing restyle (Task 7) → auth
  split-screen (Task 8). `UnlockPage` is explicitly covered by Task 2's
  automatic inheritance (documented in Global Constraints) — no gap.
- **Type consistency:** `ClicksLineChart({ data: DailyClickPoint[] })` signature
  defined in Task 4 is unchanged from the original and reused as-is in Task 6
  (`AnalyticsPage`, `LinkDetailPage` keep their existing calls, untouched).
  `useTheme()` return shape defined once in Task 3, consumed identically in
  Task 5 and Task 8. `DashboardPreviewMockup` (no props) defined once in
  Task 5, consumed identically in Task 8.
- **No placeholders:** every step has complete, exact code or an exact
  find/replace snippet with real line references.
