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
