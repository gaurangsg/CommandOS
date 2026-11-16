import { useParams, Link } from 'react-router-dom';
import { Box, Button, Chip, Stack, Typography } from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';

const moduleNames: Record<string, string> = {
  projects: 'Projects',
  tasks: 'My tasks',
  people: 'People',
  workflows: 'Workflows',
  expenses: 'Expenses',
  assets: 'Assets',
  analytics: 'Analytics',
};

export function ModulePage() {
  const { module = '' } = useParams();
  const title = moduleNames[module] ?? 'Workspace';

  return (
    <Box className="module-page">
      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 3 }}>
        <Button component={Link} to="/" startIcon={<ArrowBackRoundedIcon />} color="inherit" size="small">Overview</Button>
        <Typography color="text.disabled">/</Typography>
        <Typography variant="body2" fontWeight={600}>{title}</Typography>
      </Stack>
      <Chip icon={<AutoAwesomeRoundedIcon />} label="In the works" className="welcome-chip" sx={{ mb: 2.5 }} />
      <Typography variant="h4">{title}</Typography>
      <Typography color="text.secondary" sx={{ mt: 1.2, maxWidth: 540, lineHeight: 1.75 }}>
        {title} is part of the CommandOS rollout. This workspace is being built in focused, verified phases so every feature is connected to real services—not sample data.
      </Typography>
      <Box className="module-preview">
        <Box className="module-preview-mark"><AutoAwesomeRoundedIcon /></Box>
        <Typography variant="subtitle1" sx={{ mt: 2 }}>A thoughtful workspace is on its way</Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.6 }}>The foundation is ready. This module will come online in a future phase.</Typography>
      </Box>
    </Box>
  );
}
