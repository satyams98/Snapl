// Module-level in-memory holder so the access token never touches localStorage/sessionStorage (XSS-resistant).
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}
