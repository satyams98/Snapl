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
