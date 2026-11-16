import { z } from 'zod';
import { apiBaseUrl } from './client';

const AuthUserSchema = z.object({
  userId: z.string().uuid(),
  email: z.string().email(),
  displayName: z.string(),
  organizationId: z.string().uuid(),
  organizationName: z.string(),
  organizationSlug: z.string(),
  role: z.string(),
  permissions: z.array(z.string()),
});

const AuthSessionSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.literal('Bearer'),
  expiresIn: z.number().positive(),
  user: AuthUserSchema,
});

const InvitationPreviewSchema = z.object({
  email: z.string().email(),
  displayName: z.string(),
  organizationName: z.string(),
  organizationSlug: z.string(),
  departmentName: z.string().nullable(),
  expiresAt: z.string().datetime({ offset: true }),
});

const ErrorResponseSchema = z.object({
  error: z.object({
    code: z.string(),
    message: z.string(),
  }),
});

function parseApiResponse<T extends z.ZodType>(payload: unknown, dataSchema: T): z.infer<T> {
  const envelope = z.object({
    success: z.literal(true),
    data: z.unknown(),
    timestamp: z.string().datetime({ offset: true }),
  }).parse(payload);
  return dataSchema.parse(envelope.data);
}

export type AuthUser = z.infer<typeof AuthUserSchema>;
export type AuthSession = z.infer<typeof AuthSessionSchema>;

export interface RegisterInput {
  displayName: string;
  email: string;
  password: string;
  organizationName: string;
  organizationSlug: string;
}

export interface LoginInput {
  email: string;
  password: string;
  organizationSlug: string;
}

export type InvitationPreview = z.infer<typeof InvitationPreviewSchema>;

async function postAuth<T extends z.ZodType>(
  path: string,
  body: unknown,
  responseSchema: T,
): Promise<z.infer<T>> {
  const response = await fetch(`${apiBaseUrl}/auth/${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

  const payload: unknown = await response.json();
  if (!response.ok) {
    const error = ErrorResponseSchema.safeParse(payload);
    throw new Error(error.success ? error.data.error.message : `Authentication failed (${response.status}).`);
  }

  return parseApiResponse(payload, responseSchema);
}

export function register(input: RegisterInput): Promise<AuthSession> {
  return postAuth('register', input, AuthSessionSchema);
}

export function login(input: LoginInput): Promise<AuthSession> {
  return postAuth('login', input, AuthSessionSchema);
}

export function refreshSession(organizationSlug: string): Promise<AuthSession> {
  return postAuth('refresh', { organizationSlug }, AuthSessionSchema);
}

export function logout(): Promise<void> {
  return postAuth('logout', {}, z.object({ status: z.literal('logged_out') })).then(() => undefined);
}

export async function previewInvitation(token: string): Promise<InvitationPreview> {
  const response = await fetch(
    `${apiBaseUrl}/auth/invitations/preview?token=${encodeURIComponent(token)}`,
    { headers: { Accept: 'application/json' } },
  );
  const payload: unknown = await response.json();
  if (!response.ok) {
    const error = ErrorResponseSchema.safeParse(payload);
    throw new Error(error.success ? error.data.error.message : `Invitation request failed (${response.status}).`);
  }
  return parseApiResponse(payload, InvitationPreviewSchema);
}

export function acceptInvitation(input: {
  token: string;
  displayName: string;
  password: string;
}): Promise<AuthSession> {
  return postAuth('invitations/accept', input, AuthSessionSchema);
}

export function getCurrentUser(accessToken: string, signal?: AbortSignal): Promise<AuthUser> {
  return fetch(`${apiBaseUrl}/auth/me`, {
    headers: { Accept: 'application/json', Authorization: `Bearer ${accessToken}` },
    credentials: 'include',
    signal,
  }).then(async (response) => {
    const payload: unknown = await response.json();
    if (!response.ok) {
      const error = ErrorResponseSchema.safeParse(payload);
      throw new Error(error.success ? error.data.error.message : `Unable to load account (${response.status}).`);
    }
    return parseApiResponse(payload, AuthUserSchema);
  });
}
