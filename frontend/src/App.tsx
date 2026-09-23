import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { AppShell } from "@/layouts/AppShell";
import LoginPage from "@/pages/auth/LoginPage";
import RegisterPage from "@/pages/auth/RegisterPage";
import DashboardPage from "@/pages/DashboardPage";
import LinksPage from "@/pages/LinksPage";
import LinkDetailPage from "@/pages/LinkDetailPage";
import AnalyticsPage from "@/pages/AnalyticsPage";
import DomainsPage from "@/pages/DomainsPage";
import ApiKeysPage from "@/pages/ApiKeysPage";
import UnlockPage from "@/pages/UnlockPage";
import BillingPage from "@/pages/BillingPage";

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/unlock/:code" element={<UnlockPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppShell />}>
                <Route path="dashboard" element={<DashboardPage />} />
                <Route index element={<LinksPage />} />
                <Route path="links/:code" element={<LinkDetailPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="domains" element={<DomainsPage />} />
                <Route path="api-keys" element={<ApiKeysPage />} />
                <Route path="billing" element={<BillingPage />} />
              </Route>
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;

