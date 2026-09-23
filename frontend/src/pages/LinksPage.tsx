import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import { LinkFormDialog } from "@/components/links/LinkFormDialog";
import { bulkAction, listFolders, listUrls, type UrlSummary } from "@/api/urls";

const PAGE_SIZE = 20;

export default function LinksPage() {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [status, setStatus] = useState<"" | "active" | "disabled">("");
  const [folderId, setFolderId] = useState("");
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingLink, setEditingLink] = useState<UrlSummary | undefined>(undefined);

  const queryClient = useQueryClient();

  useEffect(() => {
    const timeout = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(timeout);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch, status, folderId]);

  const foldersQuery = useQuery({ queryKey: ["folders"], queryFn: listFolders });

  const urlsQuery = useQuery({
    queryKey: ["urls", { search: debouncedSearch, status, folderId, page }],
    queryFn: () =>
      listUrls({
        search: debouncedSearch || undefined,
        status: status || undefined,
        folderId: folderId ? Number(folderId) : undefined,
        page,
        size: PAGE_SIZE,
      }),
  });

  const bulkDisableMutation = useMutation({
    mutationFn: () => bulkAction({ shortCodes: Array.from(selected), action: "DISABLE" }),
    onSuccess: () => {
      setSelected(new Set());
      void queryClient.invalidateQueries({ queryKey: ["urls"] });
    },
  });

  const items = urlsQuery.data?.items ?? [];
  const totalCount = urlsQuery.data?.totalCount ?? 0;
  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));

  function toggleSelected(code: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(code)) {
        next.delete(code);
      } else {
        next.add(code);
      }
      return next;
    });
  }

  function toggleSelectAll() {
    setSelected((prev) => (prev.size === items.length ? new Set() : new Set(items.map((item) => item.shortCode))));
  }

  function openCreateDialog() {
    setEditingLink(undefined);
    setDialogOpen(true);
  }

  function openEditDialog(link: UrlSummary) {
    setEditingLink(link);
    setDialogOpen(true);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Links</h1>
        <Button onClick={openCreateDialog}>New link</Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Input
          placeholder="Search by short code or destination…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs"
        />
        <select
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          value={status}
          onChange={(e) => setStatus(e.target.value as "" | "active" | "disabled")}
        >
          <option value="">All statuses</option>
          <option value="active">Active</option>
          <option value="disabled">Disabled</option>
        </select>
        <select
          className="h-10 rounded-md border border-input bg-background px-3 text-sm"
          value={folderId}
          onChange={(e) => setFolderId(e.target.value)}
        >
          <option value="">All folders</option>
          {(foldersQuery.data ?? []).map((folder) => (
            <option key={folder.id} value={folder.id}>
              {folder.name}
            </option>
          ))}
        </select>
      </div>

      {selected.size > 0 && (
        <div className="flex items-center gap-3 rounded-md border bg-muted/40 px-4 py-2 text-sm">
          <span>{selected.size} selected</span>
          <Button
            size="sm"
            variant="destructive"
            disabled={bulkDisableMutation.isPending}
            onClick={() => bulkDisableMutation.mutate()}
          >
            Disable selected
          </Button>
        </div>
      )}

      <Card className="overflow-hidden">
        <table className="w-full text-sm">
          <thead className="border-b bg-muted/40 text-left text-muted-foreground">
            <tr>
              <th className="w-10 px-4 py-3">
                <input
                  type="checkbox"
                  checked={items.length > 0 && selected.size === items.length}
                  onChange={toggleSelectAll}
                  aria-label="Select all links on this page"
                />
              </th>
              <th className="px-4 py-3">Short link</th>
              <th className="px-4 py-3">Destination</th>
              <th className="px-4 py-3">Folder</th>
              <th className="px-4 py-3">Tags</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {urlsQuery.isLoading && (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-muted-foreground">
                  Loading…
                </td>
              </tr>
            )}
            {!urlsQuery.isLoading && items.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-muted-foreground">
                  No links yet. Create your first one.
                </td>
              </tr>
            )}
            {items.map((item) => (
              <tr key={item.shortCode} className="border-b last:border-b-0">
                <td className="px-4 py-3 align-top">
                  <input
                    type="checkbox"
                    checked={selected.has(item.shortCode)}
                    onChange={() => toggleSelected(item.shortCode)}
                    aria-label={`Select ${item.shortCode}`}
                  />
                </td>
                <td className="px-4 py-3 align-top font-medium">
                  <a href={item.shortUrl} target="_blank" rel="noreferrer" className="text-primary hover:underline">
                    {item.shortUrl.replace(/^https?:\/\//, "")}
                  </a>
                </td>
                <td className="max-w-xs truncate px-4 py-3 align-top text-muted-foreground" title={item.longUrl}>
                  {item.longUrl}
                </td>
                <td className="px-4 py-3 align-top">{item.folderName ?? "—"}</td>
                <td className="px-4 py-3 align-top">
                  <div className="flex flex-wrap gap-1">
                    {item.tags.map((tag) => (
                      <Badge key={tag} variant="secondary">
                        {tag}
                      </Badge>
                    ))}
                  </div>
                </td>
                <td className="px-4 py-3 align-top">
                  <Badge variant={item.disabled ? "destructive" : "default"}>
                    {item.disabled ? "Disabled" : "Active"}
                  </Badge>
                </td>
                <td className="px-4 py-3 align-top text-right">
                  <Button size="sm" variant="ghost" onClick={() => openEditDialog(item)}>
                    Edit
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <span>
          Page {page + 1} of {totalPages} · {totalCount} link{totalCount === 1 ? "" : "s"}
        </span>
        <div className="flex gap-2">
          <Button size="sm" variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <Button
            size="sm"
            variant="outline"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      </div>

      <LinkFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        link={editingLink}
        folders={foldersQuery.data ?? []}
      />
    </div>
  );
}
