export interface SecurityAlert {
  id: number;
  type: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH';
  message: string;
  relatedEmail: string | null;
  relatedAccountNumber?: string | null;
  resolved: boolean;
  createdAt: string;
  source: 'auth' | 'account';
}
