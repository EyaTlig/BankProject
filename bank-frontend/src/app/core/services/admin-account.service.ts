import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AdminAccount,
  AdminClient,
  AdminCreateAccountRequest,
  AdminTransaction,
  AdminTransactionFilters,
  CreditAccountRequest
} from '../models/admin-account.model';

@Injectable({ providedIn: 'root' })
export class AdminAccountService {

  private readonly baseUrl = 'http://localhost:8082/api/admin';

  constructor(private http: HttpClient) {}

  getAllClients(): Observable<AdminClient[]> {
    return this.http.get<AdminClient[]>(`${this.baseUrl}/clients`);
  }

  getAllAccounts(): Observable<AdminAccount[]> {
    return this.http.get<AdminAccount[]>(`${this.baseUrl}/accounts`);
  }

  createAccount(request: AdminCreateAccountRequest): Observable<AdminAccount> {
    return this.http.post<AdminAccount>(`${this.baseUrl}/accounts`, request);
  }

  creditAccount(accountId: number, request: CreditAccountRequest): Observable<AdminAccount> {
    return this.http.post<AdminAccount>(`${this.baseUrl}/accounts/${accountId}/credit`, request);
  }

  getTransactions(filters: AdminTransactionFilters): Observable<AdminTransaction[]> {
    let params = new HttpParams();
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.accountNumber) params = params.set('accountNumber', filters.accountNumber);

    return this.http.get<AdminTransaction[]>(`${this.baseUrl}/transactions`, { params });
  }
}
