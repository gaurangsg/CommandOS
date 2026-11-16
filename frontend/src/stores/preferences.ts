import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface PreferencesState {
  mode: 'light' | 'dark';
  toggleMode: () => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      mode: 'light',
      toggleMode: () => set((state) => ({ mode: state.mode === 'light' ? 'dark' : 'light' })),
    }),
    { name: 'commandos-preferences' },
  ),
);
