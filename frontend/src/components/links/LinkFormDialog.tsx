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
import { Select } from "@/components/ui/select";
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
  const [startsAt, setStartsAt] = useState("");
  const [password, setPassword] = useState("");
  const [removePassword, setRemovePassword] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setLongUrl(link?.longUrl ?? "");
      setCustomAlias("");
      setFolderId("");
      setTags(link?.tags.join(", ") ?? "");
      setStartsAt(link?.startsAt ? link.startsAt.slice(0, 16) : "");
      setPassword("");
      setRemovePassword(false);
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
      const parsedStartsAt = startsAt ? new Date(startsAt).toISOString() : undefined;

      if (isEditMode && link) {
        return updateUrl(link.shortCode, {
          longUrl,
          folderId: parsedFolderId,
          tags: parsedTags,
          startsAt: parsedStartsAt,
          password: removePassword ? "" : password || undefined,
        });
      }

      const created = await shortenUrl({
        longUrl,
        customAlias: customAlias || undefined,
        startsAt: parsedStartsAt,
        password: password || undefined,
      });
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
              <Select id="folderId" className="w-full" value={folderId} onChange={(e) => setFolderId(e.target.value)}>
                <option value="">No folder</option>
                {folders.map((folder) => (
                  <option key={folder.id} value={folder.id}>
                    {folder.name}
                  </option>
                ))}
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="tags">Tags (comma-separated)</Label>
              <Input id="tags" placeholder="launch, social" value={tags} onChange={(e) => setTags(e.target.value)} />
            </div>

            <div className="space-y-2">
              <Label htmlFor="startsAt">Activate at (optional)</Label>
              <Input
                id="startsAt"
                type="datetime-local"
                value={startsAt}
                onChange={(e) => setStartsAt(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">The link returns 404 until this time. Leave blank to activate immediately.</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">
                {isEditMode && link?.passwordProtected ? "New password (optional)" : "Password protection (optional)"}
              </Label>
              <Input
                id="password"
                type="password"
                placeholder="Leave blank for no password"
                value={password}
                disabled={removePassword}
                onChange={(e) => setPassword(e.target.value)}
              />
              {isEditMode && link?.passwordProtected && (
                <label className="flex items-center gap-2 text-sm text-muted-foreground">
                  <input
                    type="checkbox"
                    checked={removePassword}
                    onChange={(e) => setRemovePassword(e.target.checked)}
                  />
                  Remove password protection
                </label>
              )}
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
