export type UserRole = 'CLIENT' | 'ADMIN';

export interface AdminUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
}

export interface CreateClientRequest {
  firstName: string;
  lastName: string;
  email: string;
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
