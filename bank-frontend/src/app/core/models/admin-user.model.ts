export type UserRole = 'CLIENT' | 'ADMIN';

export type AdminPermission =
  | 'USERS_VIEW' | 'USERS_MANAGE'
  | 'ACCOUNTS_VIEW' | 'ACCOUNTS_MANAGE'
  | 'CREDITS_VIEW' | 'CREDITS_VALIDATE';

export interface PermissionModule {
  label: string;
  viewPermission: AdminPermission;
  actionPermission: AdminPermission;
  actionLabel: string;
}

export const PERMISSION_MODULES: PermissionModule[] = [
  { label: 'Utilisateurs', viewPermission: 'USERS_VIEW', actionPermission: 'USERS_MANAGE', actionLabel: 'Gérer' },
  { label: 'Comptes & transactions', viewPermission: 'ACCOUNTS_VIEW', actionPermission: 'ACCOUNTS_MANAGE', actionLabel: 'Gérer' },
  { label: 'Crédits', viewPermission: 'CREDITS_VIEW', actionPermission: 'CREDITS_VALIDATE', actionLabel: 'Valider' }
];

export interface AdminUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
  permissions: AdminPermission[];
}

export interface UpdatePermissionsRequest {
  permissions: AdminPermission[];
}

export interface CreateClientRequest {
  firstName: string;
  lastName: string;
  email: string;
}

export interface CreateEmployeeRequest {
  firstName: string;
  lastName: string;
  email: string;
  permissions: AdminPermission[];
}

export interface UpdateUserStatusRequest {
  enabled: boolean;
}

export interface UpdateUserRoleRequest {
  role: UserRole;
}

export interface ResetPasswordResponse {
  message: string;
}

export interface AuditLogEntry {
  id: number;
  action: string;
  targetEmail: string | null;
  performedBy: string;
  details: string | null;
  createdAt: string;
}
