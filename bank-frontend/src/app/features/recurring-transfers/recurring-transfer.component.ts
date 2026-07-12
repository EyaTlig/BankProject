import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { RecurringTransferService } from '../../core/services/recurring-transfer.service';
import { Account } from '../../core/models/account.model';
import { RecurringTransferResponse } from '../../core/models/recurring-transfer.model';

@Component({
  selector: 'app-recurring-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './recurring-transfer.component.html',
  styleUrl: './recurring-transfer.component.css'
})
export class RecurringTransferComponent implements OnInit {

  recurringTransfers: RecurringTransferResponse[] = [];
  accounts: Account[] = [];
  loading = true;
  showForm = false;
  formLoading = false;
  errorMessage: string | null = null;
  actionPendingId: number | null = null;

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private recurringTransferService: RecurringTransferService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      sourceAccountId: ['', Validators.required],
      destinationAccountNumber: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      label: [''],
      frequency: ['MONTHLY', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['']
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;

    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.form.patchValue({ sourceAccountId: accounts[0].id });
        }
        this.cdr.detectChanges();
      }
    });

    this.recurringTransferService.getMyRecurringTransfers().subscribe({
      next: (transfers) => {
        this.recurringTransfers = transfers;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  openForm(): void {
    this.showForm = true;
    this.errorMessage = null;
  }

  closeForm(): void {
    this.showForm = false;
    this.form.reset({ frequency: 'MONTHLY', sourceAccountId: this.accounts[0]?.id });
  }

  submitForm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.formLoading = true;
    this.errorMessage = null;

    const value = this.form.value;
    const request = {
      sourceAccountId: Number(value.sourceAccountId),
      destinationAccountNumber: value.destinationAccountNumber,
      amount: Number(value.amount),
      label: value.label || undefined,
      frequency: value.frequency,
      startDate: value.startDate,
      endDate: value.endDate || undefined
    };

    this.recurringTransferService.createRecurringTransfer(request).subscribe({
      next: (created) => {
        this.formLoading = false;
        this.recurringTransfers = [created, ...this.recurringTransfers];
        this.closeForm();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.formLoading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue.';
        this.cdr.detectChanges();
      }
    });
  }

  cancelTransfer(id: number): void {
    if (!confirm('Voulez-vous vraiment annuler ce virement permanent ?')) {
      return;
    }

    this.actionPendingId = id;
    this.recurringTransferService.cancelRecurringTransfer(id).subscribe({
      next: (updated) => {
        const index = this.recurringTransfers.findIndex(t => t.id === id);
        if (index !== -1) {
          this.recurringTransfers[index] = updated;
        }
        this.actionPendingId = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.actionPendingId = null;
        this.cdr.detectChanges();
      }
    });
  }

  pauseTransfer(id: number): void {
    this.actionPendingId = id;
    this.recurringTransferService.pauseRecurringTransfer(id).subscribe({
      next: (updated) => this.applyUpdate(updated),
      error: () => { this.actionPendingId = null; this.cdr.detectChanges(); }
    });
  }

  resumeTransfer(id: number): void {
    this.actionPendingId = id;
    this.recurringTransferService.resumeRecurringTransfer(id).subscribe({
      next: (updated) => this.applyUpdate(updated),
      error: () => { this.actionPendingId = null; this.cdr.detectChanges(); }
    });
  }

  private applyUpdate(updated: RecurringTransferResponse): void {
    const index = this.recurringTransfers.findIndex(t => t.id === updated.id);
    if (index !== -1) {
      this.recurringTransfers[index] = updated;
    }
    this.actionPendingId = null;
    this.cdr.detectChanges();
  }

  frequencyLabel(frequency: string): string {
    switch (frequency) {
      case 'WEEKLY': return 'Hebdomadaire';
      case 'MONTHLY': return 'Mensuel';
      case 'QUARTERLY': return 'Trimestriel';
      case 'YEARLY': return 'Annuel';
      default: return frequency;
    }
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'ACTIF';
      case 'PAUSED': return 'EN PAUSE';
      case 'CANCELLED': return 'ANNULÉ';
      case 'COMPLETED': return 'TERMINÉ';
      default: return status;
    }
  }
}
