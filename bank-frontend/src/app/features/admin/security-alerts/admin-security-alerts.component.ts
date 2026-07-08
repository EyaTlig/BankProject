import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SecurityAlertService } from '../../../core/services/security-alert.service';
import { SecurityAlert } from '../../../core/models/security-alert.model';

@Component({
  selector: 'app-admin-security-alerts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-security-alerts.component.html',
  styleUrl: './admin-security-alerts.component.css'
})
export class AdminSecurityAlertsComponent implements OnInit {

  alerts = signal<SecurityAlert[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  resolvingId = signal<number | null>(null);

  private readonly typeLabels: Record<string, string> = {
    BRUTE_FORCE_LOGIN: 'Tentatives de connexion suspectes',
    UNAUTHORIZED_ACCESS_ATTEMPT: 'Tentative d\'accès non autorisée',
    UNUSUAL_AMOUNT: 'Montant de virement inhabituel',
    HIGH_FREQUENCY_TRANSFERS: 'Fréquence de virements anormale'
  };

  constructor(private securityAlertService: SecurityAlertService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.securityAlertService.getAllAlerts().subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les alertes de sécurité.');
        this.loading.set(false);
      }
    });
  }

  resolve(alert: SecurityAlert): void {
    this.resolvingId.set(alert.id);
    this.securityAlertService.resolveAlert(alert).subscribe({
      next: (updated) => {
        this.alerts.update((list) =>
          list.map((a) => (a.id === updated.id && a.source === updated.source ? updated : a))
        );
        this.resolvingId.set(null);
      },
      error: () => {
        this.resolvingId.set(null);
      }
    });
  }

  typeLabel(type: string): string {
    return this.typeLabels[type] || type;
  }

  unresolvedCount(): number {
    return this.alerts().filter((a) => !a.resolved).length;
  }
}
