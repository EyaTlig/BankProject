import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SimulationRequest,
  SimulationResponse,
  CreateCreditRequest,
  CreditRequestResponse
} from '../models/credit.model';

@Injectable({ providedIn: 'root' })
export class CreditService {

  private readonly baseUrl = 'http://localhost:8083/api/credits';

  constructor(private http: HttpClient) {}

  simulate(request: SimulationRequest): Observable<SimulationResponse> {
    return this.http.post<SimulationResponse>(`${this.baseUrl}/simulate`, request);
  }

  createRequest(request: CreateCreditRequest): Observable<CreditRequestResponse> {
    return this.http.post<CreditRequestResponse>(`${this.baseUrl}/requests`, request);
  }

  getMyRequests(): Observable<CreditRequestResponse[]> {
    return this.http.get<CreditRequestResponse[]>(`${this.baseUrl}/requests/my`);
  }
}
