import { ChangeDetectorRef, Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {Router, RouterLink} from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { TransferService } from '../../core/services/transfer.service';
import { Account } from '../../core/models/account.model';

type TransferStep = 'details' | 'confirmation';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './transfer.component.html',
  styleUrl: './transfer.component.css'
})
export class TransferComponent implements OnInit {

  // Etat réactif en signals (compatible mode zoneless).
  // Le rafraîchissement de la vue après chaque appel HTTP est
  // assuré globalement par zoneless-refresh.interceptor.ts.
  step = signal<TransferStep>('details');
  accounts = signal<Account[]>([]);
  selectedAccount = signal<Account | undefined>(undefined);
  transferId = signal<number | null>(null);

  detailsForm: FormGroup;
  otpForm: FormGroup;

  loading = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private transferService: TransferService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.detailsForm = this.fb.group({
      sourceAccountId: ['', Validators.required],
      // Un RIB valide fait exactement 20 chiffres (07 + 807 + 13 chiffres + clé 2 chiffres)
      destinationAccountNumber: ['', [Validators.required, Validators.pattern(/^\d{20}$/)]],
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
        this.accounts.set(accounts);
        if (accounts.length > 0) {
          this.detailsForm.patchValue({ sourceAccountId: accounts[0].id });
          this.selectedAccount.set(accounts[0]);
        }
        this.cdr.detectChanges();
      }
    });
  }

  onAccountChange(): void {
    const id = Number(this.detailsForm.value.sourceAccountId);
    this.selectedAccount.set(this.accounts().find(a => a.id === id));
  }

  submitDetails(): void {
    if (this.loading()) {
      return; // évite le double-submit (double-clic, double ngSubmit)
    }

    if (this.detailsForm.invalid) {
      this.detailsForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const request = {
      sourceAccountId: Number(this.detailsForm.value.sourceAccountId),
      destinationAccountNumber: this.detailsForm.value.destinationAccountNumber,
      amount: Number(this.detailsForm.value.amount),
      label: this.detailsForm.value.label || undefined
    };

    this.transferService.initiateTransfer(request).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.transferId.set(response.transferId);
        this.step.set('confirmation');
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Une erreur est survenue.');
        this.cdr.detectChanges();
      }
    });
  }

  submitConfirmation(): void {
    if (this.loading()) {
      return;
    }

    const currentTransferId = this.transferId();
    if (this.otpForm.invalid || currentTransferId === null) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.transferService.confirmTransfer({
      transferId: currentTransferId,
      otpCode: this.otpForm.value.otpCode
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMessage.set('Virement effectué avec succès.');
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/dashboard']), 1800);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Code incorrect, veuillez réessayer.');
        this.cdr.detectChanges();
      }
    });
  }

  backToDetails(): void {
    this.step.set('details');
    this.errorMessage.set(null);
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
