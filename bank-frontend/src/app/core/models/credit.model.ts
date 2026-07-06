export type CreditType = 'PERSONNEL' | 'IMMOBILIER' | 'AUTO' | 'PROFESSIONNEL';

export type CreditStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface UpdateCreditStatusRequest {
  status: CreditStatus;
  adminComment?: string;
}

export interface SimulationRequest {
  amount: number;
  durationMonths: number;
  interestRate: number;
}

export interface ScheduleRow {
  month: number;
  monthlyPayment: number;
  principal: number;
  interest: number;
  remainingBalance: number;
}

export interface SimulationResponse {
  monthlyPayment: number;
  totalCost: number;
  totalInterest: number;
  schedule: ScheduleRow[];
}

export interface CreateCreditRequest {
  type: CreditType;
  amount: number;
  durationMonths: number;
  interestRate: number;
  purpose?: string;
}

export interface CreditRequestResponse {
  id: number;
  type: CreditType;
  amount: number;
  durationMonths: number;
  interestRate: number;
  monthlyPayment: number;
  purpose: string | null;
  status: CreditStatus;
  createdAt: string;
  updatedAt: string | null;
  adminComment: string | null;
}

export interface CreditTypeOption {
  value: CreditType;
  label: string;
  icon: string;
  defaultRate: number;
  minAmount: number;
  maxAmount: number;
  minDuration: number;
  maxDuration: number;
}

export const CREDIT_TYPE_OPTIONS: CreditTypeOption[] = [
  {
    value: 'PERSONNEL',
    label: 'Crédit personnel',
    icon: 'ti-wallet',
    defaultRate: 8.9,
    minAmount: 1000,
    maxAmount: 40000,
    minDuration: 6,
    maxDuration: 84
  },
  {
    value: 'AUTO',
    label: 'Crédit auto',
    icon: 'ti-car',
    defaultRate: 7.5,
    minAmount: 5000,
    maxAmount: 80000,
    minDuration: 12,
    maxDuration: 84
  },
  {
    value: 'IMMOBILIER',
    label: 'Crédit immobilier',
    icon: 'ti-home',
    defaultRate: 6.2,
    minAmount: 20000,
    maxAmount: 400000,
    minDuration: 24,
    maxDuration: 300
  },
  {
    value: 'PROFESSIONNEL',
    label: 'Crédit professionnel',
    icon: 'ti-briefcase',
    defaultRate: 9.5,
    minAmount: 5000,
    maxAmount: 200000,
    minDuration: 12,
    maxDuration: 120
  }
];
