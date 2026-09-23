import { apiFetch } from "./client";

export interface UrlSummary {
  shortCode: string;
  shortUrl: string;
  longUrl: string;
  createdAt: string;
  expiresAt: string | null;
  disabled: boolean;
  folderName: string | null;
  tags: string[];
  startsAt: string | null;
  passwordProtected: boolean;
}

export interface UrlListResponse {
  items: UrlSummary[];
  totalCount: number;
  page: number;
  pageSize: number;
}

export interface Folder {
  id: number;
  name: string;
}

export interface Tag {
  id: number;
  name: string;
}

export interface ListUrlsParams {
  search?: string;
  folderId?: number;
  status?: "active" | "disabled";
  tag?: string;
  page?: number;
  size?: number;
}

export interface UpdateUrlPayload {
  longUrl?: string;
  expiresAt?: string;
  folderId?: number;
  tags?: string[];
  startsAt?: string;
  password?: string;
}

export interface ShortenPayload {
  longUrl: string;
  customAlias?: string;
  startsAt?: string;
  password?: string;
}

export interface BulkActionPayload {
  shortCodes: string[];
  action: "DISABLE" | "MOVE_TO_FOLDER" | "ADD_TAGS";
  folderId?: number;
  tags?: string[];
}

export interface BulkActionResult {
  requested: number;
  succeeded: number;
}

function buildQuery(params: ListUrlsParams): string {
  const query = new URLSearchParams();
  if (params.search) query.set("search", params.search);
  if (params.folderId) query.set("folderId", String(params.folderId));
  if (params.status) query.set("status", params.status);
  if (params.tag) query.set("tag", params.tag);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  return query.toString();
}

export function listUrls(params: ListUrlsParams): Promise<UrlListResponse> {
  return apiFetch<UrlListResponse>(`/api/urls?${buildQuery(params)}`);
}

export function getUrl(shortCode: string): Promise<UrlSummary> {
  return apiFetch<UrlSummary>(`/api/urls/${shortCode}`);
}

export function updateUrl(shortCode: string, payload: UpdateUrlPayload): Promise<UrlSummary> {
  return apiFetch<UrlSummary>(`/api/urls/${shortCode}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export function shortenUrl(payload: ShortenPayload): Promise<{ shortCode: string; shortUrl: string; longUrl: string }> {
  return apiFetch("/shorten", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function disableUrl(shortCode: string): Promise<void> {
  return apiFetch(`/${shortCode}`, { method: "DELETE" });
}

export function unlockLink(shortCode: string, password: string): Promise<{ longUrl: string }> {
  return apiFetch<{ longUrl: string }>(`/${shortCode}/unlock`, {
    method: "POST",
    body: JSON.stringify({ password }),
  });
}

export function bulkAction(payload: BulkActionPayload): Promise<BulkActionResult> {
  return apiFetch<BulkActionResult>("/api/urls/bulk", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function listFolders(): Promise<Folder[]> {
  return apiFetch<Folder[]>("/api/folders");
}

export function createFolder(name: string): Promise<Folder> {
  return apiFetch<Folder>("/api/folders", {
    method: "POST",
    body: JSON.stringify({ name }),
  });
}

export function listTags(): Promise<Tag[]> {
  return apiFetch<Tag[]>("/api/tags");
}
