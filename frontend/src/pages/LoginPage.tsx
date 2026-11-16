import { useState } from 'react';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Alert, Box, Button, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../api/auth';
import { ORGANIZATION_SLUG_STORAGE_KEY, useAuthStore } from '../stores/auth';

const LoginSchema = z.object({
  email: z.string().email('Enter a valid email address.').max(254),
  password: z.string().min(1, 'Enter your password.').max(72),
  organizationSlug: z.string().min(3, 'Enter your workspace address.').max(80),
});

type LoginValues = z.infer<typeof LoginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [requestError, setRequestError] = useState('');
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(LoginSchema),
    defaultValues: { email: '', password: '', organizationSlug: localStorage.getItem(ORGANIZATION_SLUG_STORAGE_KEY) ?? '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setRequestError('');
    try {
      const session = await login(values);
      localStorage.setItem(ORGANIZATION_SLUG_STORAGE_KEY, session.user.organizationSlug);
      setSession(session);
      navigate('/', { replace: true });
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : 'Unable to sign in right now.');
    }
  });

  return (
    <Box className="auth-screen">
      <Box className="auth-brand">
        <Box className="brand-mark"><AutoAwesomeRoundedIcon sx={{ fontSize: 21 }} /></Box>
        <Typography className="brand-name">commandos</Typography>
      </Box>
      <Paper className="auth-card" elevation={0}>
        <Typography variant="h5">Welcome back</Typography>
        <Typography color="text.secondary" sx={{ mt: 0.8, mb: 3 }}>Sign in to pick up where your team left off.</Typography>
        <Stack component="form" spacing={2} onSubmit={onSubmit}>
          {requestError && <Alert severity="error">{requestError}</Alert>}
          <TextField
            label="Workspace address"
            placeholder="northstar"
            autoComplete="organization"
            {...register('organizationSlug')}
            error={Boolean(errors.organizationSlug)}
            helperText={errors.organizationSlug?.message ?? 'The address your organization chose during setup.'}
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
            label="Password"
            type="password"
            autoComplete="current-password"
            {...register('password')}
            error={Boolean(errors.password)}
            helperText={errors.password?.message}
            fullWidth
          />
          <Button type="submit" variant="contained" size="large" disabled={isSubmitting} sx={{ mt: 1 }}>
            {isSubmitting ? 'Signing in…' : 'Sign in'}
          </Button>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 3 }}>
          New to CommandOS?{' '}
          <MuiLink component={Link} to="/register" underline="hover" fontWeight={650}>Create a workspace</MuiLink>
        </Typography>
      </Paper>
      <Typography className="auth-footer">One Workspace. Every Operation. Complete Control.</Typography>
    </Box>
  );
}
