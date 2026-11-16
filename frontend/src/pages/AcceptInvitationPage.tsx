import { useEffect, useState } from 'react';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { Alert, Box, Button, CircularProgress, Link as MuiLink, Paper, Stack, TextField, Typography } from '@mui/material';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { acceptInvitation, previewInvitation } from '../api/auth';
import { ORGANIZATION_SLUG_STORAGE_KEY, useAuthStore } from '../stores/auth';

const acceptanceSchema = z.object({
  displayName: z.string().trim().min(1, 'Enter your name.').max(160),
  password: z.string().min(12, 'Use at least 12 characters.').refine(
    (value) => new TextEncoder().encode(value).length <= 72,
    'Password must not exceed 72 UTF-8 bytes.',
  ),
});

type AcceptanceValues = z.infer<typeof acceptanceSchema>;

export function AcceptInvitationPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const navigate = useNavigate();
  const setSession = useAuthStore((state) => state.setSession);
  const [requestError, setRequestError] = useState('');
  const previewQuery = useQuery({
    queryKey: ['invitation-preview', token],
    queryFn: () => previewInvitation(token),
    enabled: token.length > 0,
    retry: false,
  });
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<AcceptanceValues>({
    resolver: zodResolver(acceptanceSchema),
    defaultValues: { displayName: '', password: '' },
  });

  useEffect(() => {
    if (previewQuery.data) {
      reset({ displayName: previewQuery.data.displayName, password: '' });
    }
  }, [previewQuery.data, reset]);

  const onSubmit = handleSubmit(async ({ displayName, password }) => {
    setRequestError('');
    try {
      const session = await acceptInvitation({ token, displayName, password });
      localStorage.setItem(ORGANIZATION_SLUG_STORAGE_KEY, session.user.organizationSlug);
      setSession(session);
      navigate('/', { replace: true });
    } catch (error) {
      setRequestError(error instanceof Error ? error.message : 'Unable to accept this invitation.');
    }
  });

  return (
    <Box className="auth-screen">
      <Box className="auth-brand">
        <Box className="brand-mark"><AutoAwesomeRoundedIcon sx={{ fontSize: 21 }} /></Box>
        <Typography className="brand-name">commandos</Typography>
      </Box>
      <Paper className="auth-card" elevation={0}>
        <Typography variant="h5">Join your team</Typography>
        {previewQuery.isLoading ? (
          <Stack alignItems="center" spacing={2} sx={{ py: 5 }}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary">Verifying invitation…</Typography>
          </Stack>
        ) : previewQuery.error ? (
          <Alert severity="error" sx={{ mt: 2 }}>
            {previewQuery.error instanceof Error ? previewQuery.error.message : 'This invitation is no longer available.'}
          </Alert>
        ) : previewQuery.data ? (
          <>
            <Typography color="text.secondary" sx={{ mt: 0.8, mb: 3 }}>
              You’ve been invited to join <strong>{previewQuery.data.organizationName}</strong>
              {previewQuery.data.departmentName ? ` in ${previewQuery.data.departmentName}` : ''}.
            </Typography>
            <Stack component="form" spacing={2} onSubmit={onSubmit}>
              {requestError && <Alert severity="error">{requestError}</Alert>}
              <TextField label="Work email" value={previewQuery.data.email} InputProps={{ readOnly: true }} fullWidth />
              <TextField
                label="Full name"
                autoComplete="name"
                {...register('displayName')}
                error={Boolean(errors.displayName)}
                helperText={errors.displayName?.message}
                fullWidth
              />
              <TextField
                label="Create a password"
                type="password"
                autoComplete="new-password"
                {...register('password')}
                error={Boolean(errors.password)}
                helperText={errors.password?.message ?? 'At least 12 characters.'}
                fullWidth
              />
              <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                {isSubmitting ? 'Joining…' : 'Accept invitation'}
              </Button>
            </Stack>
          </>
        ) : (
          <Alert severity="error" sx={{ mt: 2 }}>This invitation link is missing its security token.</Alert>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 3 }}>
          Already have a workspace?{' '}
          <MuiLink component={Link} to="/login" underline="hover" fontWeight={650}>Sign in</MuiLink>
        </Typography>
      </Paper>
      <Typography className="auth-footer">One Workspace. Every Operation. Complete Control.</Typography>
    </Box>
  );
}
