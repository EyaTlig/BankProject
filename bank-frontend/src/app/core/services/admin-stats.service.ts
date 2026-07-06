import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminStatsResponse } from '../models/admin-stats.model';

@Injectable({ providedIn: 'root' })
export class AdminStatsService {

  private readonly baseUrl = 'http://localhost:8082/api/admin';

  constructor(private http: HttpClient) {}

  getStats(): Observable<AdminStatsResponse> {
    return this.http.get<AdminStatsResponse>(`${this.baseUrl}/stats`);
  }
}
