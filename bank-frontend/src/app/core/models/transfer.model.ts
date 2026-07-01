export interface InitiateTransferRequest {
  sourceAccountId: number;
  destinationAccountNumber: string;
  amount: number;
  label?: string;
}

export interface InitiateTransferResponse {
  transferId: number;
  message: string;
}

export interface ConfirmTransferRequest {
  transferId: number;
  otpCode: string;
}

export type TransferStatus = 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'FAILED';

export interface ConfirmTransferResponse {
  transferId: number;
  status: TransferStatus;
  newBalance: number;
  message: string;
}
