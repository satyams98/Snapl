import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { apiFetch } from "@/api/client";
import { setAccessToken } from "@/api/tokenStore";

export type OrgRole = "OWNER" | "ADMIN" | "MEMBER";

export interface AuthUser {
  userId: number;
  email: string;
  orgId: number;
  organizationName: string;
  role: OrgRole;
}

interface AuthResponse extends AuthUser {
  accessToken: string;
  expiresInSeconds: number;
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name: string, organizationName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toAuthUser(response: AuthResponse): AuthUser {
  const { userId, email, orgId, organizationName, role } = response;
  return { userId, email, orgId, organizationName, role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // skipAuthRetry avoids recursing into refresh-on-401 for the refresh call itself.
    apiFetch<AuthResponse>("/api/auth/refresh", { method: "POST", skipAuthRetry: true })
      .then((response) => {
        setAccessToken(response.accessToken);
        setUser(toAuthUser(response));
      })
      .catch(() => setAccessToken(null))
      .finally(() => setIsLoading(false));
  }, []);

  async function login(email: string, password: string) {
    const response = await apiFetch<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    setAccessToken(response.accessToken);
    setUser(toAuthUser(response));
  }

  async function register(email: string, password: string, name: string, organizationName: string) {
    const response = await apiFetch<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, name, organizationName }),
    });
    setAccessToken(response.accessToken);
    setUser(toAuthUser(response));
  }

  async function logout() {
    await apiFetch("/api/auth/logout", { method: "POST" }).catch(() => undefined);
    setAccessToken(null);
    setUser(null);
  }

  return <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
