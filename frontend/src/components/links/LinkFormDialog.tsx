import { useEffect, useState, type FormEvent } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ApiError } from "@/api/client";
import { shortenUrl, updateUrl, type Folder, type UrlSummary } from "@/api/urls";

interface LinkFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  link?: UrlSummary;
  folders: Folder[];
}

export function LinkFormDialog({ open, onOpenChange, link, folders }: LinkFormDialogProps) {
  const isEditMode = Boolean(link);
  const queryClient = useQueryClient();

  const [longUrl, setLongUrl] = useState("");
  const [customAlias, setCustomAlias] = useState("");
  const [folderId, setFolderId] = useState("");
  const [tags, setTags] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setLongUrl(link?.longUrl ?? "");
      setCustomAlias("");
      setFolderId("");
      setTags(link?.tags.join(", ") ?? "");
      setError(null);
    }
  }, [open, link]);

  const mutation = useMutation({
    mutationFn: async () => {
      const parsedTags = tags
        .split(",")
        .map((tag) => tag.trim())
        .filter((tag) => tag.length > 0);
      const parsedFolderId = folderId ? Number(folderId) : undefined;

      if (isEditMode && link) {
        return updateUrl(link.shortCode, {
          longUrl,
          folderId: parsedFolderId,
          tags: parsedTags,
        });
      }

      const created = await shortenUrl({ longUrl, customAlias: customAlias || undefined });
      if (parsedFolderId !== undefined || parsedTags.length > 0) {
        await updateUrl(created.shortCode, { folderId: parsedFolderId, tags: parsedTags });
      }
      return created;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["urls"] });
      onOpenChange(false);
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    mutation.mutate();
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEditMode ? "Edit link" : "Create a new link"}</DialogTitle>
            <DialogDescription>
              {isEditMode ? "Update the destination, folder, or tags." : "Shorten a URL for your organization."}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="longUrl">Destination URL</Label>
              <Input
                id="longUrl"
                required
                placeholder="https://example.com/campaign"
                value={longUrl}
                onChange={(e) => setLongUrl(e.target.value)}
              />
            </div>

            {!isEditMode && (
              <div className="space-y-2">
                <Label htmlFor="customAlias">Custom alias (optional)</Label>
                <Input
                  id="customAlias"
                  placeholder="summer-sale"
                  value={customAlias}
                  onChange={(e) => setCustomAlias(e.target.value)}
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="folderId">Folder</Label>
              <select
                id="folderId"
                className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={folderId}
                onChange={(e) => setFolderId(e.target.value)}
              >
                <option value="">No folder</option>
                {folders.map((folder) => (
                  <option key={folder.id} value={folder.id}>
                    {folder.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="tags">Tags (comma-separated)</Label>
              <Input id="tags" placeholder="launch, social" value={tags} onChange={(e) => setTags(e.target.value)} />
            </div>

            {error && <p className="text-sm text-destructive">{error}</p>}
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Saving…" : isEditMode ? "Save changes" : "Create link"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
