import { createTheme, alpha } from '@mui/material/styles';

export function createAppTheme(mode: 'light' | 'dark') {
  const dark = mode === 'dark';
  const accent = '#6858e8';

  return createTheme({
    palette: {
      mode,
      primary: { main: accent, light: '#8a7ef2', dark: '#5040c8' },
      secondary: { main: '#18a889' },
      background: {
        default: dark ? '#11131a' : '#f5f7fb',
        paper: dark ? '#191c25' : '#ffffff',
      },
      text: {
        primary: dark ? '#f2f3fa' : '#1c2030',
        secondary: dark ? '#9ba1b3' : '#72788b',
      },
      divider: dark ? 'rgba(255,255,255,0.08)' : '#eceef4',
      success: { main: '#179f7b' },
      warning: { main: '#e5a545' },
    },
    shape: { borderRadius: 14 },
    typography: {
      fontFamily: '"Inter", "Segoe UI", "Helvetica Neue", sans-serif',
      h4: { fontWeight: 720, letterSpacing: '-0.045em' },
      h5: { fontWeight: 680, letterSpacing: '-0.035em' },
      h6: { fontWeight: 650, letterSpacing: '-0.025em' },
      subtitle1: { fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 650, letterSpacing: '-0.01em' },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: { minWidth: 360, backgroundColor: dark ? '#11131a' : '#f5f7fb' },
          '::selection': { backgroundColor: alpha(accent, 0.2) },
        },
      },
      MuiPaper: {
        styleOverrides: {
          rounded: { backgroundImage: 'none' },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: { borderRadius: 10, paddingInline: 15 },
          containedPrimary: { boxShadow: `0 7px 18px ${alpha(accent, 0.22)}` },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: { borderRadius: 10 },
        },
      },
    },
  });
}
