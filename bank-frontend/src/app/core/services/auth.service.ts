import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  VerifyOtpRequest,
  RegisterRequest,
  RegisterResponse
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly baseUrl = 'http://localhost:8081/api/auth';
  private readonly tokenKey = 'bank_platform_token';

  constructor(private http: HttpClient) {}

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request);
  }

  verifyOtp(request: VerifyOtpRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/verify-otp`, request).pipe(
      tap(response => {
        if (response.token) {
          this.setToken(response.token);
        }
      })
    );
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return decoded.role ?? null;
    } catch {
      return null;
    }
  }

  getPermissions(): string[] {
    const token = this.getToken();
    if (!token) return [];
    try {
      const payload = token.split('.')[1];
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      const permissions: string = decoded.permissions ?? '';
      return permissions.length > 0 ? permissions.split(',') : [];
    } catch {
      return [];
    }
  }

  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }
}
