import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ConfirmBulkTransferRequest,
  ConfirmBulkTransferResponse,
  InitiateBulkTransferResponse,
  InitiateManualBulkTransferRequest
} from '../models/bulk-transfer.model';

@Injectable({ providedIn: 'root' })
export class BulkTransferService {

  private readonly baseUrl = 'http://localhost:8082/api/transfers/bulk';

  constructor(private http: HttpClient) {}

  initiateBulkTransfer(sourceAccountId: number, file: File): Observable<InitiateBulkTransferResponse> {
    const formData = new FormData();
    formData.append('sourceAccountId', String(sourceAccountId));
    formData.append('file', file);
    return this.http.post<InitiateBulkTransferResponse>(`${this.baseUrl}/initiate`, formData);
  }

  initiateManualBulkTransfer(request: InitiateManualBulkTransferRequest): Observable<InitiateBulkTransferResponse> {
    return this.http.post<InitiateBulkTransferResponse>(`${this.baseUrl}/initiate-manual`, request);
  }

  confirmBulkTransfer(request: ConfirmBulkTransferRequest): Observable<ConfirmBulkTransferResponse> {
    return this.http.post<ConfirmBulkTransferResponse>(`${this.baseUrl}/confirm`, request);
  }
}
