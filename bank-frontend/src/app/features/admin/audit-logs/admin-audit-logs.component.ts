import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuditLogEntry } from '../../../core/models/admin-user.model';

@Component({
  selector: 'app-admin-audit-logs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-audit-logs.component.html',
  styleUrl: './admin-audit-logs.component.css'
})
export class AdminAuditLogsComponent implements OnInit {

  logs = signal<AuditLogEntry[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  private readonly actionLabels: Record<string, string> = {
    DESACTIVATION_UTILISATEUR: 'Désactivation utilisateur',
    REACTIVATION_UTILISATEUR: 'Réactivation utilisateur',
    MODIFICATION_ROLE: 'Changement de rôle',
    SUPPRESSION_UTILISATEUR: 'Suppression utilisateur',
    REINITIALISATION_MOT_DE_PASSE: 'Réinitialisation mot de passe'
  };

  constructor(private adminUserService: AdminUserService) {}

  ngOnInit(): void {
    this.adminUserService.getAuditLogs().subscribe({
      next: (logs) => {
        this.logs.set(logs);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger le journal d'audit.");
        this.loading.set(false);
      }
    });
  }

  actionLabel(action: string): string {
    return this.actionLabels[action] || action;
  }
}
