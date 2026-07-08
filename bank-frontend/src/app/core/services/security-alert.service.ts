import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, map, Observable, of, catchError } from 'rxjs';
import { SecurityAlert } from '../models/security-alert.model';

@Injectable({ providedIn: 'root' })
export class SecurityAlertService {
  private readonly authBaseUrl = 'http://localhost:8081/api/admin/security-alerts';
  private readonly accountBaseUrl = 'http://localhost:8082/api/admin/security-alerts';

  constructor(private http: HttpClient) {}

  getAllAlerts(): Observable<SecurityAlert[]> {
    const authAlerts$ = this.http.get<any[]>(this.authBaseUrl).pipe(
      map((alerts) => alerts.map((a) => ({ ...a, source: 'auth' as const }))),
      catchError(() => of([]))
    );

    const accountAlerts$ = this.http.get<any[]>(this.accountBaseUrl).pipe(
      map((alerts) => alerts.map((a) => ({ ...a, source: 'account' as const }))),
      catchError(() => of([]))
    );

    return forkJoin([authAlerts$, accountAlerts$]).pipe(
      map(([authAlerts, accountAlerts]) =>
        [...authAlerts, ...accountAlerts].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        )
      )
    );
  }

  getUnresolvedCount(): Observable<number> {
    const authCount$ = this.http.get<{ count: number }>(`${this.authBaseUrl}/unresolved-count`).pipe(
      map((r) => r.count),
      catchError(() => of(0))
    );
    const accountCount$ = this.http.get<{ count: number }>(`${this.accountBaseUrl}/unresolved-count`).pipe(
      map((r) => r.count),
      catchError(() => of(0))
    );

    return forkJoin([authCount$, accountCount$]).pipe(
      map(([authCount, accountCount]) => authCount + accountCount)
    );
  }

  resolveAlert(alert: SecurityAlert): Observable<SecurityAlert> {
    const base = alert.source === 'auth' ? this.authBaseUrl : this.accountBaseUrl;
    return this.http
      .patch<any>(`${base}/${alert.id}/resolve`, {})
      .pipe(map((a) => ({ ...a, source: alert.source })));
  }
}
