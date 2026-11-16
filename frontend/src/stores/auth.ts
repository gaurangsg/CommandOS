import { create } from 'zustand';
import type { AuthSession, AuthUser } from '../api/auth';

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated';

interface AuthState {
  status: AuthStatus;
  accessToken: string | null;
  expiresIn: number;
  user: AuthUser | null;
  setLoading: () => void;
  setSession: (session: AuthSession) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  accessToken: null,
  expiresIn: 0,
  user: null,
  setLoading: () => set({ status: 'loading' }),
  setSession: (session) => set({
    status: 'authenticated',
    accessToken: session.accessToken,
    expiresIn: session.expiresIn,
    user: session.user,
  }),
  clearSession: () => set({
    status: 'unauthenticated',
    accessToken: null,
    expiresIn: 0,
    user: null,
  }),
}));

export const ORGANIZATION_SLUG_STORAGE_KEY = 'commandos.organizationSlug';
