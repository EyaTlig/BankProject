import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ChatRequest, ChatResponse } from '../models/chatbot.model';

@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private readonly baseUrl = 'http://localhost:8084/api/chatbot';

  constructor(private http: HttpClient) {}

  sendMessage(message: string, history: ChatMessage[]): Observable<ChatResponse> {
    const payload: ChatRequest = { message, history };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, payload);
  }
}
