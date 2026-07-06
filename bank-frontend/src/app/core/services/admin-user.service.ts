import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminUserResponse,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest,
  ResetPasswordResponse
} from '../models/admin-user.model';

@Injectable({ providedIn: 'root' })
export class AdminUserService {

  private readonly baseUrl = 'http://localhost:8081/api/admin/users';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<AdminUserResponse[]> {
    return this.http.get<AdminUserResponse[]>(this.baseUrl);
  }

  updateStatus(id: number, request: UpdateUserStatusRequest): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.baseUrl}/${id}/status`, request);
  }

  updateRole(id: number, request: UpdateUserRoleRequest): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(`${this.baseUrl}/${id}/role`, request);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  resetPassword(id: number): Observable<ResetPasswordResponse> {
    return this.http.patch<ResetPasswordResponse>(`${this.baseUrl}/${id}/reset-password`, {});
  }
}
