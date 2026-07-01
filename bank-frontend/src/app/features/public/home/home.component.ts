import { Component, signal, computed } from '@angular/core';
import { CommonModule, registerLocaleData } from '@angular/common';
import { RouterLink } from '@angular/router';
import localeFr from '@angular/common/locales/fr';

// Enregistrer les données de localisation française
registerLocaleData(localeFr, 'fr');

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {

  readonly minAmount = 3000;
  readonly maxAmount = 60000;
  readonly minDuration = 12;
  readonly maxDuration = 84;
  readonly annualRate = 7.9;

  amount = signal(20000);
  duration = signal(36);

  monthlyPayment = computed(() => {
    const p = this.amount();
    const n = this.duration();
    const r = this.annualRate / 100 / 12;
    if (r === 0) return p / n;
    return (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
  });

  totalCost = computed(() => this.monthlyPayment() * this.duration());
  totalInterest = computed(() => this.totalCost() - this.amount());

  onAmountChange(value: string): void {
    this.amount.set(Number(value));
  }

  onDurationChange(value: string): void {
    this.duration.set(Number(value));
  }

  currentYear = new Date().getFullYear();
}
