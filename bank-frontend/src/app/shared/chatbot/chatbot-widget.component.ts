import { Component, ElementRef, ViewChild, AfterViewChecked, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ChatbotService } from '../../core/services/chatbot.service';
import { ChatMessage, ChatSuggestion } from '../../core/models/chatbot.model';

@Component({
  selector: 'app-chatbot-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chatbot-widget.component.html',
  styleUrl: './chatbot-widget.component.css',
})
export class ChatbotWidgetComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLDivElement>;

  isOpen = signal(false);
  isLoading = signal(false);
  draft = '';
  errorMessage = signal<string | null>(null);

  messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      content:
        "Bonjour 👋 Je suis Assia, l'assistante Bank Platform. Comment puis-je t'aider aujourd'hui ?",
    },
  ]);

  suggestions = signal<ChatSuggestion[]>([]);

  constructor(private chatbotService: ChatbotService, private router: Router) {}

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  toggleOpen(): void {
    this.isOpen.update(v => !v);
  }

  send(): void {
    const text = this.draft.trim();
    if (!text || this.isLoading()) {
      return;
    }

    this.errorMessage.set(null);
    this.messages.update(msgs => [...msgs, { role: 'user', content: text }]);
    this.suggestions.set([]);
    this.draft = '';
    this.isLoading.set(true);

    const historyForApi = this.messages().slice(0, -1);

    this.chatbotService.sendMessage(text, historyForApi).subscribe({
      next: (response) => {
        this.messages.update(msgs => [...msgs, { role: 'assistant', content: response.reply }]);
        this.suggestions.set(response.suggestions || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          "Le service d'assistance est momentanément indisponible. Réessaie dans un instant."
        );
        this.isLoading.set(false);
      },
    });
  }

  goTo(path: string): void {
    this.isOpen.set(false);
    this.router.navigateByUrl(path);
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesContainer) {
        const el = this.messagesContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    } catch {
      // ignore
    }
  }
}
