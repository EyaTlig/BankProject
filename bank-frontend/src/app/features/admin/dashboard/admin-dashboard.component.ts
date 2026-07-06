import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminStatsService } from '../../../core/services/admin-stats.service';
import { AdminStatsResponse } from '../../../core/models/admin-stats.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {

  stats = signal<AdminStatsResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(private adminStatsService: AdminStatsService) {}

  ngOnInit(): void {
    this.adminStatsService.getStats().subscribe({
      next: (stats) => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les statistiques.");
        this.loading.set(false);
      }
    });
  }
}
