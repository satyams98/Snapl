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
