import { Component, OnDestroy, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, switchMap, catchError, of } from 'rxjs';
import { CreditService } from '../../core/services/credit.service';
import {
  CREDIT_TYPE_OPTIONS,
  CreditType,
  CreditTypeOption,
  SimulationResponse,
  CreditRequestResponse
} from '../../core/models/credit.model';

type Tab = 'simulate' | 'my-requests';

@Component({
  selector: 'app-credits',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './credits.component.html',
  styleUrl: './credits.component.css'
})
export class CreditsComponent implements OnInit, OnDestroy {

  readonly typeOptions = CREDIT_TYPE_OPTIONS;

  activeTab = signal<Tab>('simulate');

  selectedType = signal<CreditTypeOption>(CREDIT_TYPE_OPTIONS[0]);
  amount = signal(15000);
  duration = signal(36);
  rate = signal(CREDIT_TYPE_OPTIONS[0].defaultRate);
  purpose = signal('');

  simulation = signal<SimulationResponse | null>(null);
  simulating = signal(false);
  simulationError = signal<string | null>(null);
  showFullSchedule = signal(false);

  submitting = signal(false);
  submitError = signal<string | null>(null);
  submitSuccess = signal(false);

  myRequests = signal<CreditRequestResponse[]>([]);
  loadingRequests = signal(false);
  requestsLoaded = false;

  private simulate$ = new Subject<void>();
  private subscription = new Subscription();

  constructor(private creditService: CreditService) {}

  ngOnInit(): void {
    this.subscription.add(
      this.simulate$.pipe(
        debounceTime(350),
        switchMap(() => {
          this.simulating.set(true);
          this.simulationError.set(null);
          return this.creditService.simulate({
            amount: this.amount(),
            durationMonths: this.duration(),
            interestRate: this.rate()
          }).pipe(
            catchError(() => {
              this.simulationError.set("Impossible de calculer la simulation pour le moment.");
              return of(null);
            })
          );
        })
      ).subscribe(result => {
        this.simulating.set(false);
        if (result) {
          this.simulation.set(result);
        }
      })
    );

    this.simulate$.next();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  selectTab(tab: Tab): void {
    this.activeTab.set(tab);
    if (tab === 'my-requests' && !this.requestsLoaded) {
      this.loadMyRequests();
    }
  }

  onTypeSelect(option: CreditTypeOption): void {
    this.selectedType.set(option);
    this.rate.set(option.defaultRate);

    const clampedAmount = Math.min(Math.max(this.amount(), option.minAmount), option.maxAmount);
    const clampedDuration = Math.min(Math.max(this.duration(), option.minDuration), option.maxDuration);
    this.amount.set(clampedAmount);
    this.duration.set(clampedDuration);

    this.submitSuccess.set(false);
    this.simulate$.next();
  }

  onAmountChange(value: string): void {
    this.amount.set(Number(value));
    this.submitSuccess.set(false);
    this.simulate$.next();
  }

  onDurationChange(value: string): void {
    this.duration.set(Number(value));
    this.submitSuccess.set(false);
    this.simulate$.next();
  }

  onPurposeChange(value: string): void {
    this.purpose.set(value);
  }

  toggleFullSchedule(): void {
    this.showFullSchedule.update(v => !v);
  }

  visibleSchedule() {
    const rows = this.simulation()?.schedule ?? [];
    return this.showFullSchedule() ? rows : rows.slice(0, 12);
  }

  submitCreditRequest(): void {
    this.submitting.set(true);
    this.submitError.set(null);
    this.submitSuccess.set(false);

    this.creditService.createRequest({
      type: this.selectedType().value,
      amount: this.amount(),
      durationMonths: this.duration(),
      interestRate: this.rate(),
      purpose: this.purpose() || undefined
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitSuccess.set(true);
        this.requestsLoaded = false;
      },
      error: (err) => {
        this.submitting.set(false);
        this.submitError.set(err.error?.message || "Impossible d'envoyer la demande. Réessayez.");
      }
    });
  }

  loadMyRequests(): void {
    this.loadingRequests.set(true);
    this.creditService.getMyRequests().subscribe({
      next: (requests) => {
        this.myRequests.set(requests);
        this.loadingRequests.set(false);
        this.requestsLoaded = true;
      },
      error: () => {
        this.loadingRequests.set(false);
      }
    });
  }

  typeLabel(type: CreditType): string {
    return this.typeOptions.find(t => t.value === type)?.label ?? type;
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'PENDING': return 'En attente';
      case 'APPROVED': return 'Approuvé';
      case 'REJECTED': return 'Refusé';
      default: return status;
    }
  }
}
