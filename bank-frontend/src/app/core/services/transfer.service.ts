import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  InitiateTransferRequest,
  InitiateTransferResponse,
  ConfirmTransferRequest,
  ConfirmTransferResponse
} from '../models/transfer.model';

@Injectable({ providedIn: 'root' })
export class TransferService {

  private readonly baseUrl = 'http://localhost:8082/api/transfers';

  constructor(private http: HttpClient) {}

  initiateTransfer(request: InitiateTransferRequest): Observable<InitiateTransferResponse> {
    return this.http.post<InitiateTransferResponse>(`${this.baseUrl}/initiate`, request);
  }

  confirmTransfer(request: ConfirmTransferRequest): Observable<ConfirmTransferResponse> {
    return this.http.post<ConfirmTransferResponse>(`${this.baseUrl}/confirm`, request);
  }
}
