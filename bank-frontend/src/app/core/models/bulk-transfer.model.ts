import { TransferStatus } from './transfer.model';

export interface ManualBulkTransferItem {
  destinationAccountNumber: string;
  amount: number | null;
  label: string;
}

export interface InitiateManualBulkTransferRequest {
  sourceAccountId: number;
  items: { destinationAccountNumber: string; amount: number; label: string | null }[];
}

export interface InitiateBulkTransferResponse {
  bulkTransferId: number;
  totalItems: number;
  message: string;
}

export interface ConfirmBulkTransferRequest {
  bulkTransferId: number;
  otpCode: string;
}

export type BulkTransferItemStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

export interface BulkTransferItemResult {
  destinationAccountNumber: string;
  amount: number;
  status: BulkTransferItemStatus;
  errorMessage?: string;
}

export interface ConfirmBulkTransferResponse {
  bulkTransferId: number;
  status: TransferStatus;
  successCount: number;
  failedCount: number;
  results: BulkTransferItemResult[];
}
