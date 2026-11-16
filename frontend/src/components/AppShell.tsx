import { useEffect, useMemo, useRef, useState, type ChangeEvent, type KeyboardEvent, type MouseEvent as ReactMouseEvent, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  Alert, Avatar, Badge, Box, Chip, Divider, Drawer, IconButton, InputAdornment, List,
  ListItemButton, ListItemIcon, ListItemText, Menu, MenuItem, Stack, TextField, Tooltip,
  Snackbar, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import Groups2OutlinedIcon from '@mui/icons-material/Groups2Outlined';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import BarChartRoundedIcon from '@mui/icons-material/BarChartRounded';
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
import HelpOutlineRoundedIcon from '@mui/icons-material/HelpOutlineRounded';
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import KeyboardCommandKeyRoundedIcon from '@mui/icons-material/KeyboardCommandKeyRounded';
import KeyboardArrowDownRoundedIcon from '@mui/icons-material/KeyboardArrowDownRounded';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import ChevronLeftRoundedIcon from '@mui/icons-material/ChevronLeftRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';
import { AnimatePresence, motion } from 'framer-motion';
import { usePreferencesStore } from '../stores/preferences';
import { logout } from '../api/auth';
import { ORGANIZATION_SLUG_STORAGE_KEY, useAuthStore } from '../stores/auth';

const navGroups = [
  {
    title: 'WORKSPACE',
    items: [
      { label: 'Overview', path: '/', icon: SpaceDashboardRoundedIcon },
      { label: 'Projects', path: '/projects', icon: WorkOutlineRoundedIcon },
      { label: 'My tasks', path: '/tasks', icon: CheckCircleOutlineRoundedIcon },
    ],
  },
  {
    title: 'PEOPLE & PROCESS',
    items: [
      { label: 'People', path: '/people', icon: Groups2OutlinedIcon },
      { label: 'Workflows', path: '/workflows', icon: AccountTreeOutlinedIcon },
      { label: 'Expenses', path: '/expenses', icon: PaymentsOutlinedIcon },
      { label: 'Assets', path: '/assets', icon: Inventory2OutlinedIcon },
    ],
  },
  {
    title: 'INSIGHTS',
    items: [{ label: 'Analytics', path: '/analytics', icon: BarChartRoundedIcon }],
  },
];

interface AppShellProps {
  children: ReactNode;
  mobileOpen: boolean;
  onMobileClose: () => void;
  onMenuClick: () => void;
}

export function AppShell({ children, mobileOpen, onMobileClose, onMenuClick }: AppShellProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const location = useLocation();
  const navigate = useNavigate();
  const searchInputRef = useRef<HTMLInputElement>(null);
  const mode = usePreferencesStore((state) => state.mode);
  const toggleMode = usePreferencesStore((state) => state.toggleMode);
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const [query, setQuery] = useState('');
  const [searchFocused, setSearchFocused] = useState(false);
  const [workspaceAnchor, setWorkspaceAnchor] = useState<HTMLElement | null>(null);
  const [logoutError, setLogoutError] = useState('');

  const allItems = useMemo(() => navGroups.flatMap((group) => group.items), []);
  const matches = query.trim()
    ? allItems.filter((item) => item.label.toLowerCase().includes(query.trim().toLowerCase())).slice(0, 5)
    : [];
  const isSearching = searchFocused && query.trim().length > 0;

  useEffect(() => {
    const handleShortcut = (event: globalThis.KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };

    window.addEventListener('keydown', handleShortcut);
    return () => window.removeEventListener('keydown', handleShortcut);
  }, []);

  const handleSearchKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' && matches[0]) {
      navigate(matches[0].path);
      setQuery('');
      setSearchFocused(false);
    }
    if (event.key === 'Escape') {
      setQuery('');
      setSearchFocused(false);
    }
  };

  const handleLogout = async () => {
    setLogoutError('');
    try {
      await logout();
      localStorage.removeItem(ORGANIZATION_SLUG_STORAGE_KEY);
      clearSession();
      navigate('/login', { replace: true });
    } catch (error) {
      setLogoutError(error instanceof Error ? error.message : 'Unable to sign out right now.');
    }
  };

  const firstName = user?.displayName.split(' ')[0] ?? 'Your account';
  const organizationName = user?.organizationName ?? 'Your workspace';

  const sidebar = (
    <Box className="sidebar-inner">
      <Box className="brand-row">
        <Box className="brand-mark"><AutoAwesomeRoundedIcon sx={{ fontSize: 21 }} /></Box>
        <Box>
          <Typography className="brand-name">commandos</Typography>
          <Typography className="brand-caption">OPERATIONS OS</Typography>
        </Box>
        {isMobile && <IconButton onClick={onMobileClose} sx={{ ml: 'auto' }} aria-label="Close navigation"><ChevronLeftRoundedIcon /></IconButton>}
      </Box>

      <button className="workspace-switcher" onClick={(event) => setWorkspaceAnchor(event.currentTarget)}>
        <Avatar className="workspace-avatar">{organizationName.charAt(0).toUpperCase()}</Avatar>
        <Box className="workspace-copy">
          <Typography variant="body2" fontWeight={700} noWrap>{organizationName}</Typography>
          <Typography variant="caption" color="text.secondary">{user?.organizationSlug}</Typography>
        </Box>
        <KeyboardArrowDownRoundedIcon sx={{ color: 'text.secondary', fontSize: 19 }} />
      </button>
      <Menu anchorEl={workspaceAnchor} open={Boolean(workspaceAnchor)} onClose={() => setWorkspaceAnchor(null)}>
        <MenuItem disabled>{organizationName} <Chip label="Current" size="small" sx={{ ml: 1.5 }} /></MenuItem>
      </Menu>

      <List className="nav-list" disablePadding>
        {navGroups.map((group) => (
          <Box key={group.title} className="nav-group">
            <Typography className="nav-group-label">{group.title}</Typography>
            {group.items.map(({ label, path, icon: Icon }) => {
              const active = location.pathname === path || (path !== '/' && location.pathname.startsWith(path));
              return (
                <ListItemButton
                  key={path}
                  component={Link}
                  to={path}
                  onClick={isMobile ? onMobileClose : undefined}
                  selected={active}
                  className="nav-item"
                  aria-current={active ? 'page' : undefined}
                >
                  <ListItemIcon className="nav-icon"><Icon fontSize="small" /></ListItemIcon>
                  <ListItemText primary={label} />
                  {label === 'My tasks' && <Box className="nav-count">0</Box>}
                </ListItemButton>
              );
            })}
          </Box>
        ))}
      </List>

      <Box className="sidebar-bottom">
        <Box className="upgrade-card">
          <Box className="upgrade-glow" />
          <Box className="upgrade-icon"><AutoAwesomeRoundedIcon sx={{ fontSize: 17 }} /></Box>
          <Typography variant="body2" fontWeight={700} sx={{ mt: 1.5 }}>Your command center</Typography>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.5, mt: 0.5 }}>
            A clearer view of work starts here.
          </Typography>
          <Chip label="EARLY ACCESS" size="small" className="early-chip" />
        </Box>
        <List disablePadding>
          <ListItemButton className="nav-item compact-nav" disabled>
            <ListItemIcon className="nav-icon"><SettingsOutlinedIcon fontSize="small" /></ListItemIcon>
            <ListItemText primary="Settings" />
          </ListItemButton>
          <ListItemButton className="nav-item compact-nav" disabled>
            <ListItemIcon className="nav-icon"><HelpOutlineRoundedIcon fontSize="small" /></ListItemIcon>
            <ListItemText primary="Help & support" />
          </ListItemButton>
        </List>
        <Divider sx={{ my: 1.5 }} />
        <Stack direction="row" spacing={1.2} alignItems="center" className="profile-row">
          <Avatar className="profile-avatar">{user?.displayName.charAt(0).toUpperCase()}</Avatar>
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="body2" fontWeight={650} noWrap>{firstName}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap>{user?.role}</Typography>
          </Box>
          <Box className="online-indicator" title="Online" />
          <Tooltip title="Sign out">
            <IconButton size="small" onClick={handleLogout} aria-label="Sign out"><LogoutRoundedIcon fontSize="small" /></IconButton>
          </Tooltip>
        </Stack>
      </Box>
    </Box>
  );

  return (
    <Box className="app-root">
      {!isMobile && <Box component="aside" className="sidebar">{sidebar}</Box>}
      <Drawer
        open={isMobile && mobileOpen}
        onClose={onMobileClose}
        PaperProps={{ className: 'mobile-drawer' }}
      >
        {sidebar}
      </Drawer>

      <Box className="main-column">
        <Box component="header" className="topbar">
          <Stack direction="row" spacing={1.25} alignItems="center" sx={{ flex: 1, minWidth: 0 }}>
            {isMobile && <IconButton onClick={onMenuClick} aria-label="Open navigation"><MenuRoundedIcon /></IconButton>}
            <Box className={`search-wrap ${isSearching ? 'search-active' : ''}`}>
              <TextField
                size="small"
                inputRef={searchInputRef}
                placeholder="Search anything..."
                value={query}
                onChange={(event: ChangeEvent<HTMLInputElement>) => setQuery(event.target.value)}
                onFocus={() => setSearchFocused(true)}
                onBlur={() => window.setTimeout(() => setSearchFocused(false), 120)}
                onKeyDown={handleSearchKeyDown}
                aria-label="Search navigation"
                fullWidth
                InputProps={{
                  startAdornment: <InputAdornment position="start"><SearchRoundedIcon sx={{ fontSize: 20, color: 'text.secondary' }} /></InputAdornment>,
                  endAdornment: <InputAdornment position="end"><Box className="shortcut"><KeyboardCommandKeyRoundedIcon sx={{ fontSize: 12 }} /> K</Box></InputAdornment>,
                }}
              />
              <AnimatePresence>
                {isSearching && (
                  <motion.div className="search-results" initial={{ opacity: 0, y: -5 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -5 }}>
                    {matches.length ? matches.map(({ label, path, icon: Icon }) => (
                      <button key={path} className="search-result" onMouseDown={(event: ReactMouseEvent<HTMLButtonElement>) => event.preventDefault()} onClick={() => { navigate(path); setQuery(''); setSearchFocused(false); }}>
                        <Icon sx={{ fontSize: 18 }} /><span>{label}</span><span className="search-result-hint">Open</span>
                      </button>
                    )) : <Box className="search-empty">No workspace sections match “{query}”</Box>}
                  </motion.div>
                )}
              </AnimatePresence>
            </Box>
          </Stack>
          <Stack direction="row" spacing={{ xs: 0.5, sm: 1 }} alignItems="center">
            <Chip className="environment-chip" label="PREVIEW" size="small" />
            <Tooltip title={`Switch to ${mode === 'light' ? 'dark' : 'light'} mode`}>
              <IconButton onClick={toggleMode} aria-label={`Switch to ${mode === 'light' ? 'dark' : 'light'} mode`}>
                {mode === 'light' ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
              </IconButton>
            </Tooltip>
            <Tooltip title="Notifications are coming soon">
              <IconButton aria-label="Notifications (coming soon)">
                <Badge variant="dot" color="primary" invisible><NotificationsNoneRoundedIcon /></Badge>
              </IconButton>
            </Tooltip>
          </Stack>
        </Box>

        <Box component="main" className="page-content">
          {children}
          <Typography className="footer-note">Made for teams that move work forward <span>✦</span></Typography>
        </Box>
      </Box>
      <Snackbar open={Boolean(logoutError)} autoHideDuration={6000} onClose={() => setLogoutError('')}>
        <Alert severity="error" onClose={() => setLogoutError('')}>{logoutError}</Alert>
      </Snackbar>
    </Box>
  );
}
