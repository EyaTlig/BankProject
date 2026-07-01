import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateRecurringTransferRequest,
  RecurringTransferResponse
} from '../models/recurring-transfer.model';

@Injectable({ providedIn: 'root' })
export class RecurringTransferService {

  private readonly baseUrl = 'http://localhost:8082/api/recurring-transfers';

  constructor(private http: HttpClient) {}

  getMyRecurringTransfers(): Observable<RecurringTransferResponse[]> {
    return this.http.get<RecurringTransferResponse[]>(this.baseUrl);
  }

  createRecurringTransfer(request: CreateRecurringTransferRequest): Observable<RecurringTransferResponse> {
    return this.http.post<RecurringTransferResponse>(this.baseUrl, request);
  }

  cancelRecurringTransfer(id: number): Observable<RecurringTransferResponse> {
    return this.http.delete<RecurringTransferResponse>(`${this.baseUrl}/${id}`);
  }
}
