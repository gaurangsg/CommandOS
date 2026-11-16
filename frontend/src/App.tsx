import { useEffect, useState } from 'react';
import { Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
import { AppShell } from './components/AppShell';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { AcceptInvitationPage } from './pages/AcceptInvitationPage';
import { ModulePage } from './pages/ModulePage';
import { PeoplePage } from './pages/PeoplePage';
import { RegisterPage } from './pages/RegisterPage';
import { getCurrentUser, refreshSession } from './api/auth';
import { ORGANIZATION_SLUG_STORAGE_KEY, useAuthStore } from './stores/auth';

let initialSession: Promise<void> | undefined;

function initializeSession(): Promise<void> {
  if (!initialSession) {
    initialSession = (async () => {
      const organizationSlug = localStorage.getItem(ORGANIZATION_SLUG_STORAGE_KEY);
      if (!organizationSlug) {
        useAuthStore.getState().clearSession();
        return;
      }

      try {
        const session = await refreshSession(organizationSlug);
        const user = await getCurrentUser(session.accessToken);
        useAuthStore.getState().setSession({ ...session, user });
      } catch {
        useAuthStore.getState().clearSession();
      }
    })();
  }
  return initialSession;
}

function SessionBootstrap() {
  const location = useLocation();

  useEffect(() => {
    if (location.pathname !== '/accept-invitation') {
      void initializeSession();
    }
  }, [location.pathname]);

  return null;
}

function SessionRefresh() {
  const status = useAuthStore((state) => state.status);
  const expiresIn = useAuthStore((state) => state.expiresIn);
  const user = useAuthStore((state) => state.user);
  const setSession = useAuthStore((state) => state.setSession);
  const clearSession = useAuthStore((state) => state.clearSession);

  useEffect(() => {
    if (status !== 'authenticated' || !user) {
      return;
    }

    const timer = window.setTimeout(() => {
      void refreshSession(user.organizationSlug)
        .then(setSession)
        .catch(clearSession);
    }, Math.max(30, expiresIn - 60) * 1000);

    return () => window.clearTimeout(timer);
  }, [clearSession, expiresIn, setSession, status, user]);

  return null;
}

function AuthenticatedWorkspace() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <AppShell
      mobileOpen={mobileOpen}
      onMobileClose={() => setMobileOpen(false)}
      onMenuClick={() => setMobileOpen(true)}
    >
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/people" element={<PeoplePage />} />
        <Route path="/:module" element={<ModulePage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}

function LoadingScreen() {
  return (
    <Box className="auth-loading">
      <CircularProgress size={24} />
      <Typography variant="body2" color="text.secondary">Opening your workspace…</Typography>
    </Box>
  );
}

export default function App() {
  const status = useAuthStore((state) => state.status);
  const navigate = useNavigate();
  const location = useLocation();
  const isAcceptingInvitation = location.pathname === '/accept-invitation';

  useEffect(() => {
    if (status === 'unauthenticated'
      && !['/login', '/register', '/accept-invitation'].includes(window.location.pathname)) {
      navigate('/login', { replace: true });
    }
  }, [navigate, status]);

  if (isAcceptingInvitation) {
    return <AcceptInvitationPage />;
  }

  return (
    <>
      <SessionBootstrap />
      <SessionRefresh />
      {status === 'loading' ? <LoadingScreen /> : status === 'authenticated' ? (
        <AuthenticatedWorkspace />
      ) : (
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      )}
    </>
  );
}
