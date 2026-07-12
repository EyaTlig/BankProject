import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminAccountService } from '../../../core/services/admin-account.service';
import { AdminAccount, AdminClient } from '../../../core/models/admin-account.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-accounts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-accounts.component.html',
  styleUrl: './admin-accounts.component.css'
})
export class AdminAccountsComponent implements OnInit {

  accounts = signal<AdminAccount[]>([]);
  clients = signal<AdminClient[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  showForm = signal(false);
  createLoading = signal(false);
  createError = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  creditingAccountId = signal<number | null>(null);
  creditLoading = signal(false);
  creditError = signal<string | null>(null);
  creditForm: FormGroup;

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private adminAccountService: AdminAccountService,
    public authService: AuthService
  ) {
    this.form = this.fb.group({
      clientId: ['', Validators.required],
      type: ['COURANT', Validators.required],
      initialBalance: [0, [Validators.required, Validators.min(0)]]
    });

    this.creditForm = this.fb.group({
      amount: [null, [Validators.required, Validators.min(0.001)]],
      label: ['']
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.adminAccountService.getAllAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger les comptes.");
        this.loading.set(false);
      }
    });

    this.adminAccountService.getAllClients().subscribe({
      next: (clients) => {
        this.clients.set(clients);
        if (clients.length > 0 && !this.form.value.clientId) {
          this.form.patchValue({ clientId: clients[0].id });
        }
      }
    });
  }

  toggleForm(): void {
    this.showForm.update(v => !v);
    this.createError.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.createLoading.set(true);
    this.createError.set(null);

    const value = this.form.value;

    this.adminAccountService.createAccount({
      clientId: Number(value.clientId),
      type: value.type,
      initialBalance: Number(value.initialBalance)
    }).subscribe({
      next: () => {
        this.createLoading.set(false);
        this.showForm.set(false);
        this.form.patchValue({ type: 'COURANT', initialBalance: 0 });
        this.successMessage.set('Compte créé avec succès.');
        this.loadData();
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: (err) => {
        this.createLoading.set(false);
        this.createError.set(err.error?.message || 'Une erreur est survenue lors de la création du compte.');
      }
    });
  }

  toggleCreditForm(accountId: number): void {
    if (this.creditingAccountId() === accountId) {
      this.creditingAccountId.set(null);
    } else {
      this.creditingAccountId.set(accountId);
      this.creditForm.reset({ amount: null, label: '' });
      this.creditError.set(null);
    }
  }

  submitCredit(accountId: number): void {
    if (this.creditForm.invalid) {
      this.creditForm.markAllAsTouched();
      return;
    }

    this.creditLoading.set(true);
    this.creditError.set(null);

    const value = this.creditForm.value;

    this.adminAccountService.creditAccount(accountId, {
      amount: Number(value.amount),
      label: value.label || undefined
    }).subscribe({
      next: () => {
        this.creditLoading.set(false);
        this.creditingAccountId.set(null);
        this.successMessage.set('Compte crédité avec succès.');
        this.loadData();
        setTimeout(() => this.successMessage.set(null), 4000);
      },
      error: (err) => {
        this.creditLoading.set(false);
        this.creditError.set(err.error?.message || 'Une erreur est survenue lors du crédit du compte.');
      }
    });
  }
}
