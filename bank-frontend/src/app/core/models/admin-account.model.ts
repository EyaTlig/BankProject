export type AdminAccountType = 'COURANT' | 'EPARGNE';
export type AdminTransactionType = 'DEPOT' | 'RETRAIT' | 'VIREMENT_ENTRANT' | 'VIREMENT_SORTANT';

export interface AdminClient {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  accountCount: number;
  totalBalance: number;
}

export interface AdminAccount {
  id: number;
  accountNumber: string;
  type: AdminAccountType;
  balance: number;
  createdAt: string;
  clientId: number;
  clientEmail: string;
  clientFullName: string;
}

export interface AdminCreateAccountRequest {
  clientId: number;
  type: AdminAccountType;
  initialBalance?: number;
}

export interface CreditAccountRequest {
  amount: number;
  label?: string;
}

export interface AdminTransaction {
  id: number;
  type: AdminTransactionType;
  amount: number;
  balanceAfter: number;
  date: string;
  accountNumber: string;
  clientEmail: string;
  clientFullName: string;
}

export interface AdminTransactionFilters {
  startDate?: string;
  endDate?: string;
  type?: AdminTransactionType;
  accountNumber?: string;
}
