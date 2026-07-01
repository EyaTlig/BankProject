export type AccountType = 'COURANT' | 'EPARGNE';

export interface Account {
  id: number;
  accountNumber: string;
  type: AccountType;
  balance: number;
  createdAt: string;
}

export type TransactionType = 'DEPOT' | 'RETRAIT' | 'VIREMENT_ENTRANT' | 'VIREMENT_SORTANT';

export interface Transaction {
  id: number;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  date: string;
}
