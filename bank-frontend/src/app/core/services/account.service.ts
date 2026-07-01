import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account, Transaction } from '../models/account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {

  private readonly baseUrl = 'http://localhost:8082/api/accounts';

  constructor(private http: HttpClient) {}

  getMyAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(this.baseUrl);
  }

  getAccountTransactions(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.baseUrl}/${accountId}/transactions`);
  }
}
