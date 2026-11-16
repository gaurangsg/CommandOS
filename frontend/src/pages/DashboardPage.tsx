import { useQuery } from '@tanstack/react-query';
import { Alert, Box, Chip, CircularProgress, Grid, Paper, Stack, Typography, alpha, useTheme } from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import BoltRoundedIcon from '@mui/icons-material/BoltRounded';
import Groups2OutlinedIcon from '@mui/icons-material/Groups2Outlined';
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded';
import WorkOutlineRoundedIcon from '@mui/icons-material/WorkOutlineRounded';
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { fetchHealth } from '../api/client';
import { fetchEmployees } from '../api/people';
import { useAuthStore } from '../stores/auth';

const MotionPaper = motion.create(Paper);
const MotionLink = motion(Link);

const quickLinks = [
  { title: 'Projects', description: 'Bring priorities and delivery into focus.', icon: WorkOutlineRoundedIcon, color: '#6858e8', path: '/projects' },
  { title: 'People', description: 'See the people behind the work.', icon: Groups2OutlinedIcon, color: '#179f7b', path: '/people' },
  { title: 'My tasks', description: 'Keep your next steps in one place.', icon: TaskAltRoundedIcon, color: '#e5a545', path: '/tasks' },
  { title: 'Workflows', description: 'Make recurring work flow smoothly.', icon: AccountTreeOutlinedIcon, color: '#df6f91', path: '/workflows' },
];

function Greeting({ name }: { name: string }) {
  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  const dateLabel = new Intl.DateTimeFormat(undefined, { weekday: 'long', month: 'long', day: 'numeric' }).format(new Date());
  return <>{greeting}, {name.split(' ')[0]}<span className="wave">✦</span><Typography component="span" className="date-label">{dateLabel}</Typography></>;
}

export function DashboardPage() {
  const theme = useTheme();
  const health = useQuery({ queryKey: ['api-health'], queryFn: ({ signal }) => fetchHealth(signal), retry: 1 });
  const user = useAuthStore((state) => state.user);
  const peopleSummary = useQuery({
    queryKey: ['people-summary', user?.organizationId],
    queryFn: ({ signal }) => fetchEmployees({ page: 0, size: 1 }, signal),
    enabled: Boolean(user),
  });

  return (
    <Box>
      <Stack className="page-heading" direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'flex-end' }} spacing={2}>
        <Box>
          <Typography variant="h4" className="greeting"><Greeting name={user?.displayName ?? 'there'} /></Typography>
          <Typography color="text.secondary" sx={{ mt: 1 }}>Here’s your workspace at a glance.</Typography>
        </Box>
        <Chip icon={<BoltRoundedIcon />} label="Your workspace is taking shape" className="welcome-chip" />
      </Stack>

      {health.isError && (
        <Alert severity="warning" variant="outlined" sx={{ mt: 3, borderRadius: 3 }}>
          The CommandOS API is not reachable right now. The workspace preview is available, but live services need to be started.
        </Alert>
      )}

      <Grid container spacing={2.2} sx={{ mt: 1 }}>
        {[
          { label: 'Active projects', value: '—', note: 'Connect your work to see progress', icon: WorkOutlineRoundedIcon, tone: '#6858e8' },
          { label: 'Open tasks', value: '—', note: 'Your team’s next steps, together', icon: TaskAltRoundedIcon, tone: '#179f7b' },
          {
            label: 'People',
            value: peopleSummary.isLoading ? '—' : peopleSummary.data?.totalItems ?? '—',
            note: peopleSummary.isError ? 'Team directory unavailable' : 'Active workspace members',
            icon: Groups2OutlinedIcon,
            tone: '#e5a545',
          },
        ].map((item, index) => {
          const Icon = item.icon;
          return (
            <Grid key={item.label} size={{ xs: 12, sm: 4 }}>
              <MotionPaper
                className="metric-card"
                elevation={0}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: index * 0.08 }}
              >
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                  <Typography className="metric-label">{item.label}</Typography>
                  <Box className="metric-icon" sx={{ color: item.tone, bgcolor: alpha(item.tone, theme.palette.mode === 'dark' ? 0.17 : 0.1) }}><Icon sx={{ fontSize: 19 }} /></Box>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="baseline" sx={{ mt: 2 }}>
                  <Typography variant="h4" className="metric-value">{item.value}</Typography>
                </Stack>
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.8, display: 'block' }}>{item.note}</Typography>
              </MotionPaper>
            </Grid>
          );
        })}
      </Grid>

      <Grid container spacing={2.2} sx={{ mt: 0.15 }}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Paper className="overview-card" elevation={0}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box>
                <Typography variant="h6">Workspace pulse</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>A live view of progress across your organization</Typography>
              </Box>
              <Chip label="Last 30 days" size="small" variant="outlined" className="period-chip" />
            </Stack>
            <Box className="chart-empty">
              <Box className="chart-illustration" aria-hidden="true">
                <Box className="chart-line chart-line-one" />
                <Box className="chart-line chart-line-two" />
                <Box className="chart-line chart-line-three" />
                <Box className="chart-axis" />
                <Box className="chart-spark spark-one" />
                <Box className="chart-spark spark-two" />
                <Box className="chart-spark spark-three" />
                <Box className="chart-path" />
              </Box>
              <Typography variant="subtitle1" sx={{ mt: 1.2 }}>Your story starts with your team</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 370, textAlign: 'center', mt: 0.5, lineHeight: 1.65 }}>
                Once your projects and tasks are connected, your workspace pulse will appear here.
              </Typography>
            </Box>
            <Stack direction="row" spacing={2.5} className="chart-legend">
              <Box><span className="legend-dot legend-violet" /> Projects</Box>
              <Box><span className="legend-dot legend-mint" /> Tasks completed</Box>
            </Stack>
          </Paper>
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Paper className="activity-card" elevation={0}>
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Box>
                <Typography variant="h6">Recent activity</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>The latest across your workspace</Typography>
              </Box>
              <Box className="activity-live"><span /> LIVE</Box>
            </Stack>
            <Box className="activity-empty">
              <Box className="activity-orbit"><Box /><Box /><Box /><Groups2OutlinedIcon /></Box>
              <Typography variant="subtitle2" sx={{ mt: 2 }}>A little quiet for now</Typography>
              <Typography variant="caption" color="text.secondary" sx={{ maxWidth: 220, textAlign: 'center', mt: 0.5, lineHeight: 1.6 }}>
                Your team’s updates will find a home here as work gets underway.
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>

      <Box className="section-heading">
        <Box>
          <Typography variant="h6">Your command center</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.45 }}>Jump into the parts of work that matter today.</Typography>
        </Box>
        <Typography variant="caption" color="text.secondary">4 spaces</Typography>
      </Box>
      <Grid container spacing={1.8}>
        {quickLinks.map((item, index) => {
          const Icon = item.icon;
          return (
            <Grid key={item.title} size={{ xs: 12, sm: 6, lg: 3 }}>
              <MotionLink
                to={item.path}
                className="quick-link-card"
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, delay: 0.2 + index * 0.05 }}
                whileHover={{ y: -3 }}
              >
                <Box className="quick-link-top">
                  <Box className="quick-link-icon" sx={{ color: item.color, bgcolor: alpha(item.color, 0.1) }}><Icon sx={{ fontSize: 21 }} /></Box>
                  <ArrowForwardRoundedIcon className="quick-link-arrow" sx={{ fontSize: 18 }} />
                </Box>
                <Typography variant="subtitle1" sx={{ mt: 2 }}>{item.title}</Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5, fontSize: 13, lineHeight: 1.6 }}>{item.description}</Typography>
              </MotionLink>
            </Grid>
          );
        })}
      </Grid>

      {health.isLoading && <Box sx={{ position: 'fixed', bottom: 20, right: 24 }}><CircularProgress size={16} aria-label="Checking API status" /></Box>}
      {health.data && (
        <Box className="api-status" role="status">
          <span className="status-dot" />
          {health.data.service} connected
        </Box>
      )}
    </Box>
  );
}
