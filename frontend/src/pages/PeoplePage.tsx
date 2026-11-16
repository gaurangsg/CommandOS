import { useDeferredValue, useEffect, useState, type ChangeEvent, type FormEvent, type MouseEvent, type SyntheticEvent } from 'react';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import {
  Alert, Avatar, Box, Button, Chip, Dialog, DialogActions, DialogContent,
  DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Paper, Select, Skeleton,
  Stack, Tab, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination,
  TableRow, Tabs, TextField, Tooltip, Typography,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import Groups2OutlinedIcon from '@mui/icons-material/Groups2Outlined';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import { useAuthStore } from '../stores/auth';
import {
  deleteDepartment, fetchDepartments, fetchEmployees, fetchInvitations, inviteEmployee,
  revokeInvitation, saveDepartment, updateEmployeeProfile,
  type Department, type DepartmentInput, type Employee, type EmployeeProfileInput,
} from '../api/people';

const employeeFormSchema = z.object({
  jobTitle: z.string().max(120),
  departmentId: z.string(),
  managerUserId: z.string(),
  employmentType: z.enum(['FULL_TIME', 'PART_TIME', 'CONTRACTOR', 'INTERN']),
  startDate: z.string(),
});

type EmployeeFormValues = z.infer<typeof employeeFormSchema>;

const departmentFormSchema = z.object({
  name: z.string().trim().min(1, 'Enter a department name.').max(120),
  parentDepartmentId: z.string(),
});

type DepartmentFormValues = z.infer<typeof departmentFormSchema>;

const employmentTypes = [
  { value: 'FULL_TIME', label: 'Full time' },
  { value: 'PART_TIME', label: 'Part time' },
  { value: 'CONTRACTOR', label: 'Contractor' },
  { value: 'INTERN', label: 'Intern' },
] as const;

export function PeoplePage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const [tab, setTab] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [employeeToEdit, setEmployeeToEdit] = useState<Employee | null>(null);
  const [departmentToEdit, setDepartmentToEdit] = useState<Department | null>(null);
  const [departmentDialogOpen, setDepartmentDialogOpen] = useState(false);
  const [departmentToDelete, setDepartmentToDelete] = useState<Department | null>(null);
  const [inviteDialogOpen, setInviteDialogOpen] = useState(false);
  const [inviteDraft, setInviteDraft] = useState({ email: '', displayName: '', departmentId: '' });
  const [notice, setNotice] = useState('');
  const deferredSearch = useDeferredValue(search.trim());
  const canManagePeople = Boolean(user?.permissions.includes('people:manage'));
  const canManageDepartments = Boolean(user?.permissions.includes('departments:manage'));

  const departmentsQuery = useQuery({
    queryKey: ['departments', user?.organizationId],
    queryFn: ({ signal }) => fetchDepartments(signal),
    enabled: Boolean(user),
  });
  const managerCandidatesQuery = useQuery({
    queryKey: ['people-manager-options', user?.organizationId],
    queryFn: ({ signal }) => fetchEmployees({ page: 0, size: 100 }, signal),
    enabled: Boolean(user && employeeToEdit),
  });
  const employeesQuery = useQuery({
    queryKey: ['people', user?.organizationId, page, deferredSearch, departmentId],
    queryFn: ({ signal }) => fetchEmployees({
      page,
      size: 10,
      search: deferredSearch || undefined,
      departmentId: departmentId || undefined,
    }, signal),
    enabled: Boolean(user),
    placeholderData: (previous) => previous,
  });
  const invitationsQuery = useQuery({
    queryKey: ['people-invitations', user?.organizationId],
    queryFn: ({ signal }) => fetchInvitations(signal),
    enabled: Boolean(user && canManagePeople && tab === 2),
  });

  const refreshPeople = () => queryClient.invalidateQueries({ queryKey: ['people', user?.organizationId] });
  const refreshDepartments = () => {
    void queryClient.invalidateQueries({ queryKey: ['departments', user?.organizationId] });
    void refreshPeople();
  };
  const updateEmployee = useMutation({
    mutationFn: ({ id, input }: { id: string; input: EmployeeProfileInput }) => updateEmployeeProfile(id, input),
    onSuccess: () => {
      setEmployeeToEdit(null);
      setNotice('Employee profile updated.');
      void refreshPeople();
    },
  });
  const saveDepartmentMutation = useMutation({
    mutationFn: ({ id, input }: { id: string | null; input: DepartmentInput }) => saveDepartment(id, input),
    onSuccess: () => {
      setDepartmentToEdit(null);
      setDepartmentDialogOpen(false);
      setNotice('Department saved.');
      refreshDepartments();
    },
  });
  const deleteDepartmentMutation = useMutation({
    mutationFn: (id: string) => deleteDepartment(id),
    onSuccess: () => {
      setDepartmentToDelete(null);
      setNotice('Department deleted.');
      refreshDepartments();
    },
  });
  const inviteMutation = useMutation({
    mutationFn: inviteEmployee,
    onSuccess: () => {
      setInviteDialogOpen(false);
      setInviteDraft({ email: '', displayName: '', departmentId: '' });
      setNotice('Invitation email sent.');
      void queryClient.invalidateQueries({ queryKey: ['people-invitations', user?.organizationId] });
    },
  });
  const revokeInvitationMutation = useMutation({
    mutationFn: revokeInvitation,
    onSuccess: () => {
      setNotice('Invitation revoked.');
      void queryClient.invalidateQueries({ queryKey: ['people-invitations', user?.organizationId] });
    },
  });

  const employees = employeesQuery.data?.items ?? [];
  const departments = departmentsQuery.data ?? [];
  const totalEmployees = employeesQuery.data?.totalItems ?? 0;
  const unassignedEmployees = employees.filter((employee) => !employee.departmentId).length;
  const departmentError = departmentsQuery.error instanceof Error ? departmentsQuery.error.message : '';
  const peopleError = employeesQuery.error instanceof Error ? employeesQuery.error.message : '';
  const mutationError = [
    updateEmployee.error,
    saveDepartmentMutation.error,
    deleteDepartmentMutation.error,
    inviteMutation.error,
    revokeInvitationMutation.error,
  ]
    .find((error) => error instanceof Error);

  return (
    <Box className="people-page">
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'flex-end' }} spacing={2} sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">People</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.7 }}>
            Keep your team, roles, and organizational structure in sync.
          </Typography>
        </Box>
        {tab === 1 && canManageDepartments && (
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => { setDepartmentToEdit(null); setDepartmentDialogOpen(true); }}>
            Add department
          </Button>
        )}
        {tab === 2 && canManagePeople && (
          <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={() => setInviteDialogOpen(true)}>
            Invite employee
          </Button>
        )}
      </Stack>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} sx={{ mb: 2.5 }}>
        <Paper elevation={0} className="people-stat">
          <Avatar className="people-stat-icon"><Groups2OutlinedIcon fontSize="small" /></Avatar>
          <Box><Typography variant="h6">{employeesQuery.isLoading ? '—' : totalEmployees}</Typography><Typography variant="caption" color="text.secondary">Active employees</Typography></Box>
        </Paper>
        <Paper elevation={0} className="people-stat">
          <Avatar className="people-stat-icon people-stat-icon-green"><BusinessRoundedIcon fontSize="small" /></Avatar>
          <Box><Typography variant="h6">{departmentsQuery.isLoading ? '—' : departments.length}</Typography><Typography variant="caption" color="text.secondary">Departments</Typography></Box>
        </Paper>
        <Paper elevation={0} className="people-stat">
          <Avatar className="people-stat-icon people-stat-icon-amber"><Groups2OutlinedIcon fontSize="small" /></Avatar>
          <Box><Typography variant="h6">{employeesQuery.isLoading ? '—' : unassignedEmployees}</Typography><Typography variant="caption" color="text.secondary">Unassigned on this page</Typography></Box>
        </Paper>
      </Stack>

      <Paper elevation={0} className="people-panel">
        <Tabs value={tab} onChange={(_event: SyntheticEvent, value: number) => setTab(value)} aria-label="People management sections" sx={{ px: 2 }}>
          <Tab label="Employees" />
          <Tab label="Departments" />
          {canManagePeople && <Tab label="Invitations" />}
        </Tabs>
        <Box sx={{ p: { xs: 1.5, md: 2.5 } }}>
          {mutationError instanceof Error && <Alert severity="error" sx={{ mb: 2 }}>{mutationError.message}</Alert>}
          {tab === 0 ? (
            <>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ mb: 2 }}>
                <TextField
                  size="small"
                  placeholder="Search people by name or email"
                  value={search}
                  onChange={(event: ChangeEvent<HTMLInputElement>) => { setSearch(event.target.value); setPage(0); }}
                  InputProps={{ startAdornment: <SearchRoundedIcon fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} /> }}
                  sx={{ flex: 1 }}
                  inputProps={{ 'aria-label': 'Search people' }}
                />
                <FormControl size="small" sx={{ minWidth: { xs: '100%', sm: 220 } }}>
                  <InputLabel id="department-filter-label">Department</InputLabel>
                  <Select
                    labelId="department-filter-label"
                    label="Department"
                    value={departmentId}
                    onChange={(event: { target: { value: string } }) => { setDepartmentId(event.target.value); setPage(0); }}
                  >
                    <MenuItem value="">All departments</MenuItem>
                    {departments.map((department) => <MenuItem key={department.id} value={department.id}>{department.name}</MenuItem>)}
                  </Select>
                </FormControl>
              </Stack>
              {peopleError && <Alert severity="error" sx={{ mb: 2 }}>{peopleError}</Alert>}
              <EmployeeTable
                employees={employees}
                loading={employeesQuery.isLoading}
                onEdit={setEmployeeToEdit}
                canEdit={canManagePeople}
              />
              <TablePagination
                component="div"
                count={employeesQuery.data?.totalItems ?? 0}
                page={page}
                onPageChange={(_event: MouseEvent<HTMLButtonElement> | null, nextPage: number) => setPage(nextPage)}
                rowsPerPage={10}
                rowsPerPageOptions={[10]}
              />
            </>
          ) : tab === 1 ? (
            <>
              {departmentError && <Alert severity="error" sx={{ mb: 2 }}>{departmentError}</Alert>}
              <DepartmentTable
                departments={departments}
                loading={departmentsQuery.isLoading}
                canManage={canManageDepartments}
                onEdit={(department) => { setDepartmentToEdit(department); setDepartmentDialogOpen(true); }}
                onDelete={setDepartmentToDelete}
              />
            </>
          ) : (
            <>
              {invitationsQuery.error instanceof Error && (
                <Alert severity="error" sx={{ mb: 2 }}>{invitationsQuery.error.message}</Alert>
              )}
              <TableContainer className="people-table-wrap">
                <Table aria-label="Employee invitations">
                  <TableHead>
                    <TableRow>
                      <TableCell>Invitee</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Expires</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {invitationsQuery.isLoading ? (
                      <TableRow><TableCell colSpan={4}><Skeleton height={42} /></TableCell></TableRow>
                    ) : (invitationsQuery.data ?? []).length ? invitationsQuery.data?.map((invitation) => (
                      <TableRow key={invitation.id} hover>
                        <TableCell>
                          <Typography variant="body2" fontWeight={650}>{invitation.displayName}</Typography>
                          <Typography variant="caption" color="text.secondary">{invitation.email}</Typography>
                        </TableCell>
                        <TableCell><Chip size="small" label={invitation.status} variant="outlined" /></TableCell>
                        <TableCell>{new Date(invitation.expiresAt).toLocaleDateString()}</TableCell>
                        <TableCell align="right">
                          {invitation.status === 'PENDING' && (
                            <Button
                              color="error"
                              size="small"
                              disabled={revokeInvitationMutation.isPending}
                              onClick={() => revokeInvitationMutation.mutate(invitation.id)}
                            >
                              Revoke
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    )) : (
                      <TableRow><TableCell colSpan={4}>
                        <EmptyState title="No invitations yet" description="Invited teammates will appear here with their acceptance status." />
                      </TableCell></TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </>
          )}
        </Box>
      </Paper>

      <EmployeeDialog
        employee={employeeToEdit}
        departments={departments}
        employees={managerCandidatesQuery.data?.items ?? employees}
        saving={updateEmployee.isPending}
        onClose={() => { setEmployeeToEdit(null); updateEmployee.reset(); }}
        onSave={(input) => employeeToEdit && updateEmployee.mutate({ id: employeeToEdit.userId, input })}
      />
      <DepartmentDialog
        department={departmentToEdit}
        open={departmentDialogOpen}
        departments={departments}
        saving={saveDepartmentMutation.isPending}
        onClose={() => { setDepartmentDialogOpen(false); setDepartmentToEdit(null); saveDepartmentMutation.reset(); }}
        onSave={(input) => saveDepartmentMutation.mutate({ id: departmentToEdit?.id ?? null, input })}
      />
      <Dialog
        open={inviteDialogOpen}
        onClose={() => { setInviteDialogOpen(false); inviteMutation.reset(); }}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Invite a teammate</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            We’ll email a secure, single-use invitation link. New teammates join with the standard Member role.
          </Typography>
          <Stack
            component="form"
            id="invite-employee-form"
            spacing={2}
            sx={{ pt: 1 }}
            onSubmit={(event: FormEvent<HTMLFormElement>) => {
              event.preventDefault();
              inviteMutation.mutate({
                email: inviteDraft.email.trim(),
                displayName: inviteDraft.displayName.trim(),
                departmentId: inviteDraft.departmentId || null,
              });
            }}
          >
            <TextField
              label="Work email"
              type="email"
              autoComplete="email"
              required
              value={inviteDraft.email}
              onChange={(event: ChangeEvent<HTMLInputElement>) => setInviteDraft((draft) => ({ ...draft, email: event.target.value }))}
            />
            <TextField
              label="Full name"
              autoComplete="name"
              required
              value={inviteDraft.displayName}
              onChange={(event: ChangeEvent<HTMLInputElement>) => setInviteDraft((draft) => ({ ...draft, displayName: event.target.value }))}
            />
            <FormControl size="small">
              <InputLabel id="invite-department-label">Department (optional)</InputLabel>
              <Select
                labelId="invite-department-label"
                label="Department (optional)"
                value={inviteDraft.departmentId}
                onChange={(event: { target: { value: string } }) => setInviteDraft((draft) => ({ ...draft, departmentId: event.target.value }))}
              >
                <MenuItem value="">No department</MenuItem>
                {departments.map((department) => <MenuItem key={department.id} value={department.id}>{department.name}</MenuItem>)}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setInviteDialogOpen(false)}>Cancel</Button>
          <Button
            type="submit"
            form="invite-employee-form"
            variant="contained"
            disabled={inviteMutation.isPending || !inviteDraft.email.trim() || !inviteDraft.displayName.trim()}
          >
            {inviteMutation.isPending ? 'Sending…' : 'Send invitation'}
          </Button>
        </DialogActions>
      </Dialog>
      <Dialog open={Boolean(departmentToDelete)} onClose={() => setDepartmentToDelete(null)}>
        <DialogTitle>Delete department?</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary">
            {departmentToDelete?.name} can only be deleted after its employees and child departments are moved.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDepartmentToDelete(null)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            disabled={deleteDepartmentMutation.isPending}
            onClick={() => departmentToDelete && deleteDepartmentMutation.mutate(departmentToDelete.id)}
          >
            {deleteDepartmentMutation.isPending ? 'Deleting…' : 'Delete department'}
          </Button>
        </DialogActions>
      </Dialog>
      {notice && (
        <Box role="status" aria-live="polite" className="people-notice">
          <Chip label={notice} color="success" onDelete={() => setNotice('')} />
        </Box>
      )}
    </Box>
  );
}

function EmployeeTable({
  employees, loading, onEdit, canEdit,
}: {
  employees: Employee[];
  loading: boolean;
  onEdit: (employee: Employee) => void;
  canEdit: boolean;
}) {
  return (
    <TableContainer className="people-table-wrap">
      <Table aria-label="Employees" size="medium">
        <TableHead>
          <TableRow>
            <TableCell>Employee</TableCell>
            <TableCell>Role</TableCell>
            <TableCell>Department</TableCell>
            <TableCell>Employment</TableCell>
            {canEdit && <TableCell align="right">Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {loading ? Array.from({ length: 5 }, (_, index) => (
            <TableRow key={index}><TableCell colSpan={canEdit ? 5 : 4}><Skeleton height={42} /></TableCell></TableRow>
          )) : employees.length ? employees.map((employee) => (
            <TableRow key={employee.userId} hover>
              <TableCell>
                <Stack direction="row" spacing={1.2} alignItems="center">
                  <Avatar className="people-avatar">{employee.displayName.charAt(0).toUpperCase()}</Avatar>
                  <Box minWidth={0}>
                    <Typography variant="body2" fontWeight={650} noWrap>{employee.displayName}</Typography>
                    <Typography variant="caption" color="text.secondary" noWrap>{employee.jobTitle || employee.email}</Typography>
                  </Box>
                </Stack>
              </TableCell>
              <TableCell><Chip size="small" label={employee.role} variant="outlined" /></TableCell>
              <TableCell>{employee.departmentName ?? <Typography variant="body2" color="text.disabled">Unassigned</Typography>}</TableCell>
              <TableCell>{employmentTypes.find((type) => type.value === employee.employmentType)?.label ?? employee.employmentType}</TableCell>
              {canEdit && (
                <TableCell align="right">
                  <Tooltip title={`Edit ${employee.displayName}`}>
                    <IconButton size="small" aria-label={`Edit ${employee.displayName}`} onClick={() => onEdit(employee)}><EditOutlinedIcon fontSize="small" /></IconButton>
                  </Tooltip>
                </TableCell>
              )}
            </TableRow>
          )) : (
            <TableRow><TableCell colSpan={canEdit ? 5 : 4}><EmptyState title="No employees found" description="Try another search or clear the department filter." /></TableCell></TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function DepartmentTable({
  departments, loading, onEdit, onDelete, canManage,
}: {
  departments: Department[];
  loading: boolean;
  onEdit: (department: Department) => void;
  onDelete: (department: Department) => void;
  canManage: boolean;
}) {
  return (
    <TableContainer className="people-table-wrap">
      <Table aria-label="Departments">
        <TableHead>
          <TableRow>
            <TableCell>Department</TableCell>
            <TableCell>Parent department</TableCell>
            <TableCell>Employees</TableCell>
            {canManage && <TableCell align="right">Actions</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {loading ? Array.from({ length: 4 }, (_, index) => (
            <TableRow key={index}><TableCell colSpan={canManage ? 4 : 3}><Skeleton height={42} /></TableCell></TableRow>
          )) : departments.length ? departments.map((department) => (
            <TableRow key={department.id} hover>
              <TableCell><Stack direction="row" spacing={1.2} alignItems="center"><Avatar className="department-avatar"><BusinessRoundedIcon fontSize="small" /></Avatar><Typography variant="body2" fontWeight={650}>{department.name}</Typography></Stack></TableCell>
              <TableCell>{department.parentDepartmentName ?? <Typography variant="body2" color="text.disabled">Top level</Typography>}</TableCell>
              <TableCell>{department.memberCount}</TableCell>
              {canManage && (
                <TableCell align="right">
                  <Tooltip title="Edit department"><IconButton size="small" aria-label={`Edit ${department.name}`} onClick={() => onEdit(department)}><EditOutlinedIcon fontSize="small" /></IconButton></Tooltip>
                  <Tooltip title="Delete department"><IconButton size="small" aria-label={`Delete ${department.name}`} color="error" onClick={() => onDelete(department)}><DeleteOutlineRoundedIcon fontSize="small" /></IconButton></Tooltip>
                </TableCell>
              )}
            </TableRow>
          )) : (
            <TableRow><TableCell colSpan={canManage ? 4 : 3}><EmptyState title="Start with a department" description="Organize people into teams that reflect how your company works." /></TableCell></TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}

function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <Stack alignItems="center" spacing={0.75} sx={{ py: 6, textAlign: 'center' }}>
      <Groups2OutlinedIcon color="disabled" sx={{ fontSize: 28, mb: 0.5 }} />
      <Typography variant="subtitle2">{title}</Typography>
      <Typography variant="body2" color="text.secondary">{description}</Typography>
    </Stack>
  );
}

function EmployeeDialog({
  employee, departments, employees, saving, onClose, onSave,
}: {
  employee: Employee | null;
  departments: Department[];
  employees: Employee[];
  saving: boolean;
  onClose: () => void;
  onSave: (input: EmployeeProfileInput) => void;
}) {
  const { control, register, handleSubmit, reset, formState: { errors } } = useForm<EmployeeFormValues>({
    resolver: zodResolver(employeeFormSchema),
    defaultValues: { jobTitle: '', departmentId: '', managerUserId: '', employmentType: 'FULL_TIME', startDate: '' },
  });

  useEffect(() => {
    if (employee) {
      reset({
        jobTitle: employee.jobTitle ?? '',
        departmentId: employee.departmentId ?? '',
        managerUserId: employee.managerUserId ?? '',
        employmentType: employee.employmentType,
        startDate: employee.startDate ?? '',
      });
    }
  }, [employee, reset]);

  const submit = (values: EmployeeFormValues) => onSave({
    jobTitle: values.jobTitle.trim() || null,
    departmentId: values.departmentId || null,
    managerUserId: values.managerUserId || null,
    employmentType: values.employmentType,
    startDate: values.startDate || null,
  });

  return (
    <Dialog open={Boolean(employee)} onClose={onClose} fullWidth maxWidth="sm">
      <Box component="form" onSubmit={handleSubmit(submit)}>
        <DialogTitle>Edit employee profile</DialogTitle>
        <DialogContent>
          {employee && <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{employee.displayName} · {employee.email}</Typography>}
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            <TextField label="Job title" {...register('jobTitle')} error={Boolean(errors.jobTitle)} helperText={errors.jobTitle?.message} />
            <Controller
              name="departmentId"
              control={control}
              render={({ field }) => (
                <FormControl>
                  <InputLabel id="employee-department-label">Department</InputLabel>
                  <Select {...field} labelId="employee-department-label" label="Department">
                    <MenuItem value="">Unassigned</MenuItem>
                    {departments.map((department) => <MenuItem key={department.id} value={department.id}>{department.name}</MenuItem>)}
                  </Select>
                </FormControl>
              )}
            />
            <Controller
              name="managerUserId"
              control={control}
              render={({ field }) => (
                <FormControl>
                  <InputLabel id="employee-manager-label">Manager</InputLabel>
                  <Select {...field} labelId="employee-manager-label" label="Manager">
                    <MenuItem value="">No manager</MenuItem>
                    {employees.filter((candidate) => candidate.userId !== employee?.userId).map((candidate) => (
                      <MenuItem key={candidate.userId} value={candidate.userId}>{candidate.displayName}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            />
            <Controller
              name="employmentType"
              control={control}
              render={({ field }) => (
                <TextField {...field} select label="Employment type">
                  {employmentTypes.map((type) => <MenuItem key={type.value} value={type.value}>{type.label}</MenuItem>)}
                </TextField>
              )}
            />
            <TextField label="Start date" type="date" InputLabelProps={{ shrink: true }} {...register('startDate')} />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>{saving ? 'Saving…' : 'Save profile'}</Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}

function DepartmentDialog({
  department, open, departments, saving, onClose, onSave,
}: {
  department: Department | null;
  open: boolean;
  departments: Department[];
  saving: boolean;
  onClose: () => void;
  onSave: (input: DepartmentInput) => void;
}) {
  const { control, register, handleSubmit, reset, formState: { errors } } = useForm<DepartmentFormValues>({
    resolver: zodResolver(departmentFormSchema),
    defaultValues: { name: '', parentDepartmentId: '' },
  });

  useEffect(() => {
    if (!open) return;
    reset({ name: department?.name ?? '', parentDepartmentId: department?.parentDepartmentId ?? '' });
  }, [department, open, reset]);

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="xs">
      <Box component="form" onSubmit={handleSubmit((values) => onSave({
        name: values.name.trim(),
        parentDepartmentId: values.parentDepartmentId || null,
      }))}>
        <DialogTitle>{department ? 'Edit department' : 'Create a department'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            <TextField label="Department name" autoFocus {...register('name')} error={Boolean(errors.name)} helperText={errors.name?.message} />
            <Controller
              name="parentDepartmentId"
              control={control}
              render={({ field }) => (
                <FormControl>
                  <InputLabel id="parent-department-label">Parent department</InputLabel>
                  <Select {...field} labelId="parent-department-label" label="Parent department">
                    <MenuItem value="">Top level</MenuItem>
                    {departments.filter((candidate) => candidate.id !== department?.id).map((candidate) => (
                      <MenuItem key={candidate.id} value={candidate.id}>{candidate.name}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={saving}>{saving ? 'Saving…' : department ? 'Save changes' : 'Create department'}</Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
