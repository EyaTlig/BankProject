import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminUserResponse,
  CreateClientRequest,
  CreateEmployeeRequest,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest,
  UpdatePermissionsRequest,
  ResetPasswordResponse,
  AuditLogEntry
} from '../models/admin-user.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {

  private readonly baseUrl = 'http://localhost:8081/api/admin/users';
  private readonly adminBaseUrl = 'http://localhost:8081/api/admin';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<AdminUserResponse[]> {
    return this.http.get<AdminUserResponse[]>(this.baseUrl);
  }

  createClient(request: CreateClientRequest): Observable<AdminUserResponse> {
    return this.http.post<AdminUserResponse>(this.baseUrl, request);
  }

  createEmployee(request: CreateEmployeeRequest): Observable<AdminUserResponse> {
    return this.http.post<AdminUserResponse>(`${this.adminBaseUrl}/employees`, request);
  }

  updateStatus(id: number, request: UpdateUserStatusRequest): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  updateRole(id: number, request: UpdateUserRoleRequest): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.baseUrl}/${id}/role`, request);
  }

  updatePermissions(id: number, request: UpdatePermissionsRequest): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.baseUrl}/${id}/permissions`, request);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resetPassword(id: number): Observable<ResetPasswordResponse> {
    return this.http.patch<ResetPasswordResponse>(`${this.baseUrl}/${id}/reset-password`, {});
  }

  getAuditLogs(): Observable<AuditLogEntry[]> {
    return this.http.get<AuditLogEntry[]>(`${this.adminBaseUrl}/audit-logs`);
  }
}
