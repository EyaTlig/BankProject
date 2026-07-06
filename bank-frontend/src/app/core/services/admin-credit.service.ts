import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreditRequestResponse, UpdateCreditStatusRequest } from '../models/credit.model';

@Injectable({ providedIn: 'root' })
export class AdminCreditService {

  private readonly baseUrl = 'http://localhost:8083/api/admin/credits';

  constructor(private http: HttpClient) {}

  getAllRequests(): Observable<CreditRequestResponse[]> {
    return this.http.get<CreditRequestResponse[]>(`${this.baseUrl}/requests`);
  }

  updateStatus(id: number, request: UpdateCreditStatusRequest): Observable<CreditRequestResponse> {
    return this.http.patch<CreditRequestResponse>(`${this.baseUrl}/requests/${id}/status`, request);
  }
}
