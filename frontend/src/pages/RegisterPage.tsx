import { useState } from 'react';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Alert, Box, Button, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import { Link, useNavigate } from 'react-router-dom';
import { register as registerWorkspace } from '../api/auth';
import { ORGANIZATION_SLUG_STORAGE_KEY, useAuthStore } from '../stores/auth';

const RegisterSchema = z.object({
  displayName: z.string().trim().min(1, 'Enter your name.').max(160),
  email: z.string().trim().email('Enter a valid email address.').max(254),
  password: z.string().min(12, 'Use at least 12 characters.').max(72),
  organizationName: z.string().trim().min(1, 'Enter your organization name.').max(160),
  organizationSlug: z.string()
    .trim()
    .min(3, 'Use at least 3 characters.')
    .max(63, 'Use no more than 63 characters.')
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'Use lowercase letters, numbers, and hyphens.'),
});

type RegisterValues = z.infer<typeof RegisterSchema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [requestError, setRequestError] = useState('');
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterValues>({
    resolver: zodResolver(RegisterSchema),
    defaultValues: {
      displayName: '',
      email: '',
      password: '',
      organizationName: '',
      organizationSlug: '',
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    setRequestError('');
    try {
      const session = await registerWorkspace(values);
      localStorage.setItem(ORGANIZATION_SLUG_STORAGE_KEY, session.user.organizationSlug);
      setSession(session);
      navigate('/', { replace: true });
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : 'Unable to create the workspace right now.');
    }
  });

  return (
    <Box className="auth-screen auth-register-screen">
      <Box className="auth-brand">
        <Box className="brand-mark"><AutoAwesomeRoundedIcon sx={{ fontSize: 21 }} /></Box>
        <Typography className="brand-name">commandos</Typography>
      </Box>
      <Paper className="auth-card" elevation={0}>
        <Typography variant="h5">Make room for better work</Typography>
        <Typography color="text.secondary" sx={{ mt: 0.8, mb: 3 }}>Create your workspace and become its first owner.</Typography>
        <Stack component="form" spacing={1.8} onSubmit={onSubmit}>
          {requestError && <Alert severity="error">{requestError}</Alert>}
          <TextField
            label="Your name"
            autoComplete="name"
            {...register('displayName')}
            error={Boolean(errors.displayName)}
            helperText={errors.displayName?.message}
            fullWidth
          />
          <TextField
            label="Work email"
            type="email"
            autoComplete="email"
            {...register('email')}
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
            fullWidth
          />
          <TextField
            label="Organization name"
            autoComplete="organization"
            {...register('organizationName')}
            error={Boolean(errors.organizationName)}
            helperText={errors.organizationName?.message}
            fullWidth
          />
          <TextField
            label="Workspace address"
            placeholder="northstar-studio"
            autoComplete="url"
            {...register('organizationSlug')}
            error={Boolean(errors.organizationSlug)}
            helperText={errors.organizationSlug?.message ?? 'Lowercase letters, numbers, and hyphens.'}
            fullWidth
          />
          <TextField
            label="Password"
            type="password"
            autoComplete="new-password"
            {...register('password')}
            error={Boolean(errors.password)}
            helperText={errors.password?.message ?? 'At least 12 characters.'}
            fullWidth
          />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} sx={{ mt: 0.5 }}>
            {isSubmitting ? 'Creating workspace…' : 'Create workspace'}
          </Button>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 2.5 }}>
          Already have a workspace?{' '}
          <MuiLink component={Link} to="/login" underline="hover" fontWeight={650}>Sign in</MuiLink>
        </Typography>
      </Paper>
      <Typography className="auth-footer">One Workspace. Every Operation. Complete Control.</Typography>
    </Box>
  );
}
