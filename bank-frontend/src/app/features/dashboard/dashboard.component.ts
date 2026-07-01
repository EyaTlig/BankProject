import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { Account, Transaction } from '../../core/models/account.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  accounts: Account[] = [];
  recentTransactions: (Transaction & { accountNumber: string })[] = [];
  loading = true;
  errorMessage: string | null = null;

  constructor(
    private accountService: AccountService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  get totalBalance(): number {
    return this.accounts.reduce((sum, account) => sum + account.balance, 0);
  }

  get courantAccount(): Account | undefined {
    return this.accounts.find(a => a.type === 'COURANT');
  }

  get epargneAccount(): Account | undefined {
    return this.accounts.find(a => a.type === 'EPARGNE');
  }

  loadDashboard(): void {
    this.loading = true;

    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;

        if (accounts.length === 0) {
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        const transactionRequests = accounts.map(account =>
          this.accountService.getAccountTransactions(account.id)
        );

        forkJoin(transactionRequests).subscribe({
          next: (results) => {
            const allTransactions: (Transaction & { accountNumber: string })[] = [];

            results.forEach((transactions, index) => {
              transactions.forEach(t => {
                allTransactions.push({ ...t, accountNumber: accounts[index].accountNumber });
              });
            });

            this.recentTransactions = allTransactions
              .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
              .slice(0, 5);

            this.loading = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.loading = false;
            this.cdr.detectChanges();
          }
        });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger vos comptes.';
        this.cdr.detectChanges();
      }
    });
  }

  isCredit(type: string): boolean {
    return type === 'DEPOT' || type === 'VIREMENT_ENTRANT';
  }

  goToTransfers(): void {
    this.router.navigate(['/transfers']);
  }
}
