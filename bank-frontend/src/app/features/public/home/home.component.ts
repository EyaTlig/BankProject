import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import { RouterLink } from '@angular/router';
import localeFr from '@angular/common/locales/fr';
import { Subject, Subscription, debounceTime, switchMap, catchError, of } from 'rxjs';
import { CreditService } from '../../../core/services/credit.service';
import { SimulationResponse } from '../../../core/models/credit.model';

// Enregistrer les données de localisation française
registerLocaleData(localeFr, 'fr');

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {

  readonly minAmount = 3000;
  readonly maxAmount = 60000;
  readonly minDuration = 12;
  readonly maxDuration = 84;
  readonly annualRate = 7.9;

  amount = signal(20000);
  duration = signal(36);

  simulation = signal<SimulationResponse | null>(null);
  simulating = signal(false);
  simulationError = signal(false);

  currentYear = new Date().getFullYear();

  private simulate$ = new Subject<void>();
  private subscription = new Subscription();

  constructor(private creditService: CreditService) {}

  ngOnInit(): void {
    this.subscription.add(
      this.simulate$.pipe(
        debounceTime(300),
        switchMap(() => {
          this.simulating.set(true);
          this.simulationError.set(false);
          return this.creditService.simulate({
            amount: this.amount(),
            durationMonths: this.duration(),
            interestRate: this.annualRate
          }).pipe(
            catchError(() => {
              this.simulationError.set(true);
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

  onAmountChange(value: string): void {
    this.amount.set(Number(value));
    this.simulate$.next();
  }

  onDurationChange(value: string): void {
    this.duration.set(Number(value));
    this.simulate$.next();
  }

  monthlyPayment(): number {
    return this.simulation()?.monthlyPayment ?? 0;
  }

  totalCost(): number {
    return this.simulation()?.totalCost ?? 0;
  }

  totalInterest(): number {
    return this.simulation()?.totalInterest ?? 0;
  }
}
