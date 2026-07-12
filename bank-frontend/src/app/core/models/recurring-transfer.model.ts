export type RecurringFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type RecurringTransferStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED' | 'COMPLETED';

export interface CreateRecurringTransferRequest {
  sourceAccountId: number;
  destinationAccountNumber: string;
  amount: number;
  label?: string;
  frequency: RecurringFrequency;
  startDate: string;
  endDate?: string;
}

export interface UpdateRecurringTransferRequest {
  destinationAccountNumber: string;
  amount: number;
  label?: string;
  frequency: RecurringFrequency;
  endDate?: string;
}

export interface RecurringTransferResponse {
  id: number;
  destinationAccountNumber: string;
  amount: number;
  label: string | null;
  frequency: RecurringFrequency;
  startDate: string;
  endDate: string | null;
  nextExecutionDate: string;
  status: RecurringTransferStatus;
}
