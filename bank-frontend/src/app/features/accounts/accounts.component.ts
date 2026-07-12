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

    // 👈 AJOUTER : Écouter les changements des filtres
    this.filterForm.valueChanges.subscribe(() => {
      console.log('=== DEBUG Filtres changés ===', this.filterForm.value);
      this.applyFilters();
      this.cdr.detectChanges();
    });
  }

  selectAccount(account: Account): void {
    console.log('=== DEBUG selectAccount appelé pour ===', account.id);
    this.selectedAccount = account;
    this.transactionsLoading = true;
    this.transactions = [];
    this.filteredTransactions = [];

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
    // Si pas de transactions, ne rien faire
    if (this.transactions.length === 0) {
      this.filteredTransactions = [];
      return;
    }

    const { type, startDate, endDate, minAmount, maxAmount } = this.filterForm.value;

    console.log('=== DEBUG Application des filtres ===', {
      type,
      startDate,
      endDate,
      minAmount,
      maxAmount,
      totalTransactions: this.transactions.length
    });

    this.filteredTransactions = this.transactions.filter(t => {
      // Filtre par type
      if (type && t.type !== type) {
        console.log('Filtré par type:', t.type, '!==', type);
        return false;
      }

      // Filtre par date de début
      const transactionDate = new Date(t.date);
      if (startDate) {
        const start = new Date(startDate);
        start.setHours(0, 0, 0, 0);
        if (transactionDate < start) {
          console.log('Filtré par date début:', transactionDate, '<', start);
          return false;
        }
      }

      // Filtre par date de fin
      if (endDate) {
        const end = new Date(endDate);
        end.setHours(23, 59, 59, 999);
        if (transactionDate > end) {
          console.log('Filtré par date fin:', transactionDate, '>', end);
          return false;
        }
      }

      // Filtre par montant minimum
      if (minAmount && t.amount < Number(minAmount)) {
        console.log('Filtré par montant min:', t.amount, '<', minAmount);
        return false;
      }

      // Filtre par montant maximum
      if (maxAmount && t.amount > Number(maxAmount)) {
        console.log('Filtré par montant max:', t.amount, '>', maxAmount);
        return false;
      }

      return true;
    });

    console.log('=== DEBUG Transactions filtrées ===', this.filteredTransactions.length);
    this.cdr.detectChanges();
  }

  resetFilters(): void {
    console.log('=== DEBUG Reset des filtres ===');
    this.filterForm.reset({
      type: '',
      startDate: '',
      endDate: '',
      minAmount: '',
      maxAmount: ''
    });
    // applyFilters() sera appelé automatiquement par valueChanges
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
