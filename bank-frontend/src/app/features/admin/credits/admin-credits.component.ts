import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminCreditService } from '../../../core/services/admin-credit.service';
import { CreditRequestResponse, CreditType } from '../../../core/models/credit.model';

type FilterTab = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL';

@Component({
  selector: 'app-admin-credits',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-credits.component.html',
  styleUrl: './admin-credits.component.css'
})
export class AdminCreditsComponent implements OnInit {

  requests = signal<CreditRequestResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  activeFilter = signal<FilterTab>('PENDING');

  actionPendingId = signal<number | null>(null);
  commentDraft = signal<Record<number, string>>({});
  rejectingId = signal<number | null>(null);

  filteredRequests = computed(() => {
    const filter = this.activeFilter();
    const all = this.requests();
    if (filter === 'ALL') return all;
    return all.filter(r => r.status === filter);
  });

  pendingCount = computed(() => this.requests().filter(r => r.status === 'PENDING').length);

  constructor(private adminCreditService: AdminCreditService) {}

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminCreditService.getAllRequests().subscribe({
      next: (requests) => {
        this.requests.set(requests);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les demandes de crédit.");
        this.loading.set(false);
      }
    });
  }

  setFilter(filter: FilterTab): void {
    this.activeFilter.set(filter);
  }

  onCommentChange(id: number, value: string): void {
    this.commentDraft.update(map => ({ ...map, [id]: value }));
  }

  startReject(id: number): void {
    this.rejectingId.set(id);
  }

  cancelReject(): void {
    this.rejectingId.set(null);
  }

  approve(request: CreditRequestResponse): void {
    this.actionPendingId.set(request.id);
    this.adminCreditService.updateStatus(request.id, {
      status: 'APPROVED',
      adminComment: this.commentDraft()[request.id] || undefined
    }).subscribe({
      next: (updated) => this.applyUpdate(updated),
      error: () => this.handleActionError()
    });
  }

  confirmReject(request: CreditRequestResponse): void {
    this.actionPendingId.set(request.id);
    this.adminCreditService.updateStatus(request.id, {
      status: 'REJECTED',
      adminComment: this.commentDraft()[request.id] || undefined
    }).subscribe({
      next: (updated) => {
        this.applyUpdate(updated);
        this.rejectingId.set(null);
      },
      error: () => this.handleActionError()
    });
  }

  private applyUpdate(updated: CreditRequestResponse): void {
    this.requests.update(list => list.map(r => r.id === updated.id ? updated : r));
    this.actionPendingId.set(null);
  }

  private handleActionError(): void {
    this.error.set("Impossible de mettre à jour cette demande.");
    this.actionPendingId.set(null);
  }

  typeLabel(type: CreditType): string {
    switch (type) {
      case 'PERSONNEL': return 'Crédit personnel';
      case 'AUTO': return 'Crédit auto';
      case 'IMMOBILIER': return 'Crédit immobilier';
      case 'PROFESSIONNEL': return 'Crédit professionnel';
      default: return type;
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'APPROVED': return 'Approuvé';
      case 'REJECTED': return 'Refusé';
      default: return status;
    }
  }
}
