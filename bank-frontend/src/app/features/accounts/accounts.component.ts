import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { Account, Transaction, TransactionType } from '../../core/models/account.model';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './accounts.component.html',
  styleUrl: './accounts.component.css'
})
export class AccountsComponent implements OnInit {

  accounts: Account[] = [];
  selectedAccount: Account | undefined;
  transactions: Transaction[] = [];
  filteredTransactions: Transaction[] = [];

  loading = true;
  transactionsLoading = false;

  filterForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private cdr: ChangeDetectorRef
  ) {
    this.filterForm = this.fb.group({
      type: [''],
      startDate: [''],
      endDate: [''],
      minAmount: [''],
      maxAmount: ['']
    });
  }

  ngOnInit(): void {
    console.log('=== DEBUG AccountsComponent ngOnInit appelé ===');
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        console.log('=== DEBUG accounts reçus ===', accounts);
        this.accounts = accounts;
        this.loading = false;
        if (accounts.length > 0) {
          this.selectAccount(accounts[0]);
        }
        this.cdr.detectChanges();
        console.log('=== DEBUG detectChanges() appelé manuellement ===');
      },
      error: (err) => {
        console.log('=== DEBUG erreur accounts ===', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectAccount(account: Account): void {
    console.log('=== DEBUG selectAccount appelé pour ===', account.id);
    this.selectedAccount = account;
    this.transactionsLoading = true;

    this.accountService.getAccountTransactions(account.id).subscribe({
      next: (transactions) => {
        console.log('=== DEBUG transactions reçues ===', transactions);
        this.transactions = transactions;
        this.applyFilters();
        this.transactionsLoading = false;
        this.cdr.detectChanges();
        console.log('=== DEBUG detectChanges() appelé manuellement (transactions) ===');
      },
      error: (err) => {
        console.log('=== DEBUG erreur transactions ===', err);
        this.transactionsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    const { type, startDate, endDate, minAmount, maxAmount } = this.filterForm.value;

    this.filteredTransactions = this.transactions.filter(t => {
      if (type && t.type !== type) return false;

      const transactionDate = new Date(t.date);
      if (startDate && transactionDate < new Date(startDate)) return false;
      if (endDate && transactionDate > new Date(endDate + 'T23:59:59')) return false;

      if (minAmount && t.amount < Number(minAmount)) return false;
      if (maxAmount && t.amount > Number(maxAmount)) return false;

      return true;
    });
  }

  resetFilters(): void {
    this.filterForm.reset({
      type: '', startDate: '', endDate: '', minAmount: '', maxAmount: ''
    });
  }

  isCredit(type: TransactionType): boolean {
    return type === 'DEPOT' || type === 'VIREMENT_ENTRANT';
  }

  typeLabel(type: TransactionType): string {
    switch (type) {
      case 'DEPOT': return 'Dépôt';
      case 'RETRAIT': return 'Retrait';
      case 'VIREMENT_ENTRANT': return 'Virement entrant';
      case 'VIREMENT_SORTANT': return 'Virement sortant';
      default: return type;
    }
  }
}
