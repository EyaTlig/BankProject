import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AdminAccountService } from '../../../core/services/admin-account.service';
import { AdminTransaction } from '../../../core/models/admin-account.model';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-transactions.component.html',
  styleUrl: './admin-transactions.component.css'
})
export class AdminTransactionsComponent implements OnInit {

  transactions = signal<AdminTransaction[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  filterForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private adminAccountService: AdminAccountService
  ) {
    this.filterForm = this.fb.group({
      startDate: [''],
      endDate: [''],
      type: [''],
      accountNumber: ['']
    });
  }

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading.set(true);
    this.error.set(null);

    const value = this.filterForm.value;

    this.adminAccountService.getTransactions({
      startDate: value.startDate || undefined,
      endDate: value.endDate || undefined,
      type: value.type || undefined,
      accountNumber: value.accountNumber || undefined
    }).subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les transactions.");
        this.loading.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filterForm.reset({ startDate: '', endDate: '', type: '', accountNumber: '' });
    this.search();
  }
}
