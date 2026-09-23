import type { BreakdownItem } from "@/api/analytics";

export function BreakdownList({ items, emptyLabel = "No data yet" }: { items: BreakdownItem[]; emptyLabel?: string }) {
  if (items.length === 0) {
    return <p className="text-sm text-muted-foreground">{emptyLabel}</p>;
  }

  const max = Math.max(...items.map((item) => item.count));

  return (
    <ul className="space-y-2">
      {items.map((item) => (
        <li key={item.label} className="flex items-center gap-3 text-sm">
          <span className="w-24 shrink-0 truncate">{item.label}</span>
          <div className="h-2 flex-1 rounded-full bg-muted">
            <div className="h-2 rounded-full bg-primary" style={{ width: `${(item.count / max) * 100}%` }} />
          </div>
          <span className="w-8 shrink-0 text-right text-muted-foreground">{item.count}</span>
        </li>
      ))}
    </ul>
  );
}
