import { z } from 'zod';
import { apiBaseUrl } from './client';
import { useAuthStore } from '../stores/auth';

const ErrorResponseSchema = z.object({
  error: z.object({ message: z.string() }),
});

const EmployeeSchema = z.object({
  userId: z.string().uuid(),
  displayName: z.string(),
  email: z.string().email(),
  membershipStatus: z.string(),
  role: z.string(),
  jobTitle: z.string().nullable(),
  departmentId: z.string().uuid().nullable(),
  departmentName: z.string().nullable(),
  managerUserId: z.string().uuid().nullable(),
  managerName: z.string().nullable(),
  employmentType: z.enum(['FULL_TIME', 'PART_TIME', 'CONTRACTOR', 'INTERN']),
  startDate: z.string().nullable(),
});

const EmployeePageSchema = z.object({
  items: z.array(EmployeeSchema),
  page: z.number().int(),
  size: z.number().int(),
  totalItems: z.number().int(),
  totalPages: z.number().int(),
});

const DepartmentSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  parentDepartmentId: z.string().uuid().nullable(),
  parentDepartmentName: z.string().nullable(),
  memberCount: z.number().int(),
});

const InvitationSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  displayName: z.string(),
  status: z.string(),
  departmentId: z.string().uuid().nullable(),
  expiresAt: z.string().datetime({ offset: true }),
});

export type Employee = z.infer<typeof EmployeeSchema>;
export type EmployeePage = z.infer<typeof EmployeePageSchema>;
export type Department = z.infer<typeof DepartmentSchema>;
export type Invitation = z.infer<typeof InvitationSchema>;

export interface EmployeeProfileInput {
  jobTitle: string | null;
  departmentId: string | null;
  managerUserId: string | null;
  employmentType: Employee['employmentType'];
  startDate: string | null;
}

export interface DepartmentInput {
  name: string;
  parentDepartmentId: string | null;
}

export interface InviteEmployeeInput {
  email: string;
  displayName: string;
  departmentId: string | null;
}

async function request<T extends z.ZodType>(
  path: string,
  schema: T,
  init: RequestInit = {},
): Promise<z.infer<T>> {
  const accessToken = useAuthStore.getState().accessToken;
  if (!accessToken) {
    throw new Error('Your session has expired. Sign in again to continue.');
  }

  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  headers.set('Authorization', `Bearer ${accessToken}`);
  if (init.body) headers.set('Content-Type', 'application/json');

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 204) {
    return schema.parse(undefined);
  }

  const payload: unknown = await response.json();
  if (!response.ok) {
    const error = ErrorResponseSchema.safeParse(payload);
    throw new Error(error.success ? error.data.error.message : `People request failed (${response.status}).`);
  }

  const envelope = z.object({
    success: z.literal(true),
    data: z.unknown(),
    timestamp: z.string().datetime({ offset: true }),
  }).parse(payload);
  return schema.parse(envelope.data);
}

export function fetchEmployees(params: {
  page: number;
  size: number;
  search?: string;
  departmentId?: string;
}, signal?: AbortSignal): Promise<EmployeePage> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size) });
  if (params.search) query.set('search', params.search);
  if (params.departmentId) query.set('departmentId', params.departmentId);
  return request(`/people?${query}`, EmployeePageSchema, { signal });
}

export function updateEmployeeProfile(
  userId: string,
  input: EmployeeProfileInput,
): Promise<Employee> {
  return request(`/people/${userId}`, EmployeeSchema, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function fetchDepartments(signal?: AbortSignal): Promise<Department[]> {
  return request('/departments', z.array(DepartmentSchema), { signal });
}

export function saveDepartment(id: string | null, input: DepartmentInput): Promise<Department> {
  return request(id ? `/departments/${id}` : '/departments', DepartmentSchema, {
    method: id ? 'PUT' : 'POST',
    body: JSON.stringify(input),
  });
}

export function deleteDepartment(id: string): Promise<void> {
  return request(`/departments/${id}`, z.void(), { method: 'DELETE' });
}

export function fetchInvitations(signal?: AbortSignal): Promise<Invitation[]> {
  return request('/people/invitations', z.array(InvitationSchema), { signal });
}

export function inviteEmployee(input: InviteEmployeeInput): Promise<Invitation> {
  return request('/people/invitations', InvitationSchema, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function revokeInvitation(id: string): Promise<void> {
  return request(`/people/invitations/${id}`, z.void(), { method: 'DELETE' });
}
