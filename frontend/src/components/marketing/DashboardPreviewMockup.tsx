const ROW_TONE_CLASSES: Record<"success" | "info" | "warning", string> = {
  success: "bg-success",
  info: "bg-info",
  warning: "bg-warning",
};

const KPIS = [
  { label: "Clicks", value: "12.4k", className: "text-primary" },
  { label: "Links", value: "482", className: "text-success-dark" },
  { label: "Domains", value: "6", className: "text-info-dark" },
];

const ROWS: { name: string; value: string; tone: keyof typeof ROW_TONE_CLASSES }[] = [
  { name: "go.acme.com/launch", value: "1.2k", tone: "success" },
  { name: "go.acme.com/promo", value: "864", tone: "info" },
  { name: "go.acme.com/beta", value: "410", tone: "warning" },
];

export function DashboardPreviewMockup() {
  return (
    <div className="relative w-full max-w-md">
      <div className="absolute -inset-8 -z-10 rounded-[2.5rem] bg-gradient-to-br from-primary/25 via-primary/10 to-transparent blur-2xl" />
      <div
        className="overflow-hidden rounded-2xl border border-border/60 bg-card"
        style={{ boxShadow: "0 24px 64px -20px rgba(43,49,133,0.35), 0 8px 24px rgba(27,27,31,0.10)" }}
      >
        <div className="flex">
          <div className="flex w-11 shrink-0 flex-col items-center gap-3 border-r border-border/60 bg-muted/60 py-4">
            <div className="flex size-6 items-center justify-center rounded-lg bg-primary text-[10px] font-bold text-primary-foreground">
              S
            </div>
            <div className="size-2 rounded-full bg-primary" />
            <div className="size-2 rounded-full bg-muted-foreground/25" />
            <div className="size-2 rounded-full bg-muted-foreground/25" />
            <div className="size-2 rounded-full bg-muted-foreground/25" />
          </div>

          <div className="flex-1 p-4">
            <div className="mb-4 flex items-center justify-between">
              <div className="h-2.5 w-20 rounded-full bg-foreground/10" />
              <div className="size-6 rounded-full bg-primary-lighter" />
            </div>

            <div className="mb-4 grid grid-cols-3 gap-2">
              {KPIS.map((stat) => (
                <div key={stat.label} className="rounded-lg bg-muted p-2.5">
                  <p className="text-[9px] font-medium text-muted-foreground">{stat.label}</p>
                  <p className={`mt-1 text-sm font-semibold ${stat.className}`}>{stat.value}</p>
                </div>
              ))}
            </div>

            <div className="mb-4 h-24 rounded-lg bg-muted/60 p-2">
              <svg viewBox="0 0 200 60" className="h-full w-full" preserveAspectRatio="none">
                <defs>
                  <linearGradient id="mockupFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--color-primary)" stopOpacity="0.35" />
                    <stop offset="100%" stopColor="var(--color-primary)" stopOpacity="0" />
                  </linearGradient>
                </defs>
                <path
                  d="M0,45 L25,35 L50,40 L75,20 L100,28 L125,15 L150,22 L175,8 L200,14 L200,60 L0,60 Z"
                  fill="url(#mockupFill)"
                />
                <polyline
                  fill="none"
                  stroke="var(--color-primary)"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  points="0,45 25,35 50,40 75,20 100,28 125,15 150,22 175,8 200,14"
                />
                <circle cx="200" cy="14" r="3.5" fill="var(--color-primary)" />
              </svg>
            </div>

            <div className="space-y-1.5">
              {ROWS.map((row) => (
                <div key={row.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`size-1.5 rounded-full ${ROW_TONE_CLASSES[row.tone]}`} />
                    <span className="h-2 w-24 rounded-full bg-foreground/10" />
                  </div>
                  <span className="text-[10px] font-medium text-muted-foreground">{row.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
