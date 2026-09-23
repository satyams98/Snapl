import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { LineChart } from "lucide-react";
import type { DailyClickPoint } from "@/api/analytics";

function EmptyState() {
  return (
    <div className="flex h-[280px] flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-border text-center">
      <LineChart className="size-6 text-muted-foreground" />
      <p className="text-body2 font-medium">No clicks yet</p>
      <p className="max-w-[220px] text-caption text-muted-foreground">
        Share a link to start seeing activity here.
      </p>
    </div>
  );
}

function ChartTooltip({ active, payload, label }: { active?: boolean; payload?: { value: number }[]; label?: string }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-md border border-border/60 bg-card px-3 py-2 text-body2 shadow-[var(--shadow-card)]">
      <p className="text-caption text-muted-foreground">{label}</p>
      <p className="font-medium">{payload[0].value} clicks</p>
    </div>
  );
}

export function ClicksLineChart({ data }: { data: DailyClickPoint[] }) {
  const hasActivity = data.some((point) => point.clicks > 0);

  if (!hasActivity) {
    return <EmptyState />;
  }

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
        <Tooltip content={<ChartTooltip />} />
        <Area type="monotone" dataKey="clicks" stroke="var(--color-primary)" strokeWidth={2} fill="url(#clicksFill)" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
