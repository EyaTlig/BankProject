import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { BulkTransferService } from '../../core/services/bulk-transfer.service';
import { Account } from '../../core/models/account.model';
import { BulkTransferItemResult, ManualBulkTransferItem } from '../../core/models/bulk-transfer.model';

type BulkStep = 'upload' | 'confirmation' | 'results';
type BulkMode = 'csv' | 'manual';

interface CsvPreviewRow {
  destinationAccountNumber: string;
  amount: string;
  label: string;
}

@Component({
  selector: 'app-bulk-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './bulk-transfer.component.html',
  styleUrl: './bulk-transfer.component.css'
})
export class BulkTransferComponent implements OnInit {

  step: BulkStep = 'upload';
  mode: BulkMode = 'csv';
  accounts: Account[] = [];
  selectedAccount: Account | undefined;
  uploadForm: FormGroup;
  otpForm: FormGroup;
  selectedFile: File | null = null;
  previewRows: CsvPreviewRow[] = [];
  previewError: string | null = null;
  manualItems: ManualBulkTransferItem[] = [];
  bulkTransferId: number | null = null;
  totalItems = 0;
  results: BulkTransferItemResult[] = [];
  successCount = 0;
  failedCount = 0;
  loading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private bulkTransferService: BulkTransferService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.uploadForm = this.fb.group({ sourceAccountId: ['', Validators.required] });
    this.otpForm = this.fb.group({ otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]] });
    this.resetManualItems();
  }

  ngOnInit(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.accounts = accounts;
        if (accounts.length > 0) {
          this.uploadForm.patchValue({ sourceAccountId: accounts[0].id });
          this.selectedAccount = accounts[0];
        }
        this.cdr.detectChanges();
      }
    });
  }

  setMode(mode: BulkMode): void {
    this.mode = mode;
    this.errorMessage = null;
    this.previewError = null;
    this.cdr.detectChanges();
  }

  private resetManualItems(): void {
    this.manualItems = [
      { destinationAccountNumber: '', amount: null, label: '' },
      { destinationAccountNumber: '', amount: null, label: '' }
    ];
  }

  addManualRow(): void {
    this.manualItems.push({ destinationAccountNumber: '', amount: null, label: '' });
  }

  removeManualRow(index: number): void {
    this.manualItems.splice(index, 1);
    if (this.manualItems.length === 0) {
      this.resetManualItems();
    }
  }

  onAccountChange(): void {
    const id = Number(this.uploadForm.value.sourceAccountId);
    this.selectedAccount = this.accounts.find(a => a.id === id);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length > 0 ? input.files[0] : null;
    this.selectedFile = file;
    this.previewRows = [];
    this.previewError = null;
    this.errorMessage = null;
    if (!file) return;
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.previewError = 'Le fichier doit etre au format CSV.';
      this.selectedFile = null;
      this.cdr.detectChanges();
      return;
    }
    this.parseCsvPreview(file);
  }

  private parseCsvPreview(file: File): void {
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result || '');
      const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
      if (lines.length <= 1) {
        this.previewError = 'Le fichier CSV ne contient aucune ligne de donnees.';
        this.cdr.detectChanges();
        return;
      }
      this.previewRows = lines.slice(1, 21).map(line => {
        const parts = line.split(',').map(p => p.trim());
        return { destinationAccountNumber: parts[0] ?? '', amount: parts[1] ?? '', label: parts[2] ?? '' };
      });
      this.cdr.detectChanges();
    };
    reader.onerror = () => { this.previewError = 'Impossible de lire le fichier.'; this.cdr.detectChanges(); };
    reader.readAsText(file, 'UTF-8');
  }

  downloadTemplate(): void {
    const csv = 'destinationAccountNumber,amount,label\n07807XXXXXXXXXXXXXXX,100.000,Loyer\n';
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }));
    const a = document.createElement('a');
    a.href = url; a.download = 'modele_virements_groupes.csv'; a.click();
    URL.revokeObjectURL(url);
  }

  submitUpload(): void {
    if (this.mode === 'manual') {
      this.submitManualUpload();
      return;
    }
    if (this.uploadForm.invalid || !this.selectedFile) {
      this.uploadForm.markAllAsTouched();
      if (!this.selectedFile) { this.previewError = 'Veuillez selectionner un fichier CSV.'; this.cdr.detectChanges(); }
      return;
    }
    this.loading = true; this.errorMessage = null; this.cdr.detectChanges();
    this.bulkTransferService.initiateBulkTransfer(Number(this.uploadForm.value.sourceAccountId), this.selectedFile).subscribe({
      next: (response) => {
        this.loading = false;
        this.bulkTransferId = response.bulkTransferId;
        this.totalItems = response.totalItems;
        this.step = 'confirmation';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue.';
        this.cdr.detectChanges();
      }
    });
  }

  submitManualUpload(): void {
    if (this.uploadForm.invalid) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    const validItems = this.manualItems.filter(item =>
      item.destinationAccountNumber.trim().length > 0 && item.amount !== null && item.amount > 0
    );

    if (validItems.length === 0) {
      this.errorMessage = 'Ajoutez au moins un bénéficiaire valide (compte destinataire et montant).';
      this.cdr.detectChanges();
      return;
    }

    this.loading = true; this.errorMessage = null; this.cdr.detectChanges();
    this.bulkTransferService.initiateManualBulkTransfer({
      sourceAccountId: Number(this.uploadForm.value.sourceAccountId),
      items: validItems.map(item => ({
        destinationAccountNumber: item.destinationAccountNumber.trim(),
        amount: item.amount as number,
        label: item.label?.trim() || null
      }))
    }).subscribe({
      next: (response) => {
        this.loading = false;
        this.bulkTransferId = response.bulkTransferId;
        this.totalItems = response.totalItems;
        this.step = 'confirmation';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Une erreur est survenue.';
        this.cdr.detectChanges();
      }
    });
  }

  submitConfirmation(): void {
    if (this.otpForm.invalid || this.bulkTransferId === null) { this.otpForm.markAllAsTouched(); return; }
    this.loading = true; this.errorMessage = null; this.cdr.detectChanges();
    this.bulkTransferService.confirmBulkTransfer({ bulkTransferId: this.bulkTransferId, otpCode: this.otpForm.value.otpCode }).subscribe({
      next: (response) => {
        this.loading = false;
        this.results = response.results;
        this.successCount = response.successCount;
        this.failedCount = response.failedCount;
        this.step = 'results';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Code incorrect.';
        this.cdr.detectChanges();
      }
    });
  }

  backToUpload(): void { this.step = 'upload'; this.errorMessage = null; this.cdr.detectChanges(); }
  goToDashboard(): void { this.router.navigate(['/dashboard']); }

  startNewBulkTransfer(): void {
    this.step = 'upload'; this.mode = 'csv'; this.selectedFile = null; this.previewRows = [];
    this.previewError = null; this.errorMessage = null; this.bulkTransferId = null;
    this.totalItems = 0; this.results = []; this.successCount = 0; this.failedCount = 0;
    this.resetManualItems();
    this.otpForm.reset(); this.cdr.detectChanges();
  }

  get otpControl() { return this.otpForm.get('otpCode'); }
}
