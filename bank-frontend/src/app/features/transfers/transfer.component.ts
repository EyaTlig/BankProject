import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { TransferService } from '../../core/services/transfer.service';
import { Account } from '../../core/models/account.model';

type TransferStep = 'details' | 'confirmation';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.css'
})
export class TransferComponent implements OnInit {

  step: TransferStep = 'details';
  accounts: Account[] = [];
  selectedAccount: Account | undefined;
  transferId: number | null = null;

  detailsForm: FormGroup;
  otpForm: FormGroup;

  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private transferService: TransferService,
    private router: Router
  ) {
    this.detailsForm = this.fb.group({
      sourceAccountId: ['', Validators.required],
      destinationAccountNumber: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      label: ['']
    });

    this.otpForm = this.fb.group({
      otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });
  }

  ngOnInit(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.detailsForm.patchValue({ sourceAccountId: accounts[0].id });
          this.selectedAccount = accounts[0];
        }
      }
    });
  }

  onAccountChange(): void {
    const id = Number(this.detailsForm.value.sourceAccountId);
    this.selectedAccount = this.accounts.find(a => a.id === id);
  }

  submitDetails(): void {
    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const request = {
      sourceAccountId: Number(this.detailsForm.value.sourceAccountId),
      destinationAccountNumber: this.detailsForm.value.destinationAccountNumber,
      amount: Number(this.detailsForm.value.amount),
      label: this.detailsForm.value.label || undefined
    };

    this.transferService.initiateTransfer(request).subscribe({
      next: (response) => {
        console.log('=== DEBUG initiate next() appelé ===', response);
        this.loading = false;
        this.transferId = response.transferId;
        this.step = 'confirmation';
        console.log('=== DEBUG step après assignation ===', this.step);
      },
      error: (err) => {
        console.log('=== DEBUG initiate error() appelé ===', err);
        this.loading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue.';
      }
    });
  }

  submitConfirmation(): void {
    if (this.otpForm.invalid || this.transferId === null) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    this.transferService.confirmTransfer({
      transferId: this.transferId,
      otpCode: this.otpForm.value.otpCode
    }).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Virement effectué avec succès.';
        setTimeout(() => this.router.navigate(['/dashboard']), 1800);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Code incorrect, veuillez réessayer.';
      }
    });
  }

  backToDetails(): void {
    this.step = 'details';
    this.errorMessage = null;
  }

  get destinationControl() {
    return this.detailsForm.get('destinationAccountNumber');
  }

  get amountControl() {
    return this.detailsForm.get('amount');
  }

  get otpControl() {
    return this.otpForm.get('otpCode');
  }
}
