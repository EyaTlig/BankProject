export type UserRole = 'CLIENT' | 'ADMIN';

export interface AdminUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  enabled: boolean;
}

export interface UpdateUserStatusRequest {
  enabled: boolean;
}

export interface UpdateUserRoleRequest {
  role: UserRole;
}

export interface ResetPasswordResponse {
  message: string;
  temporaryPassword: string;
}
