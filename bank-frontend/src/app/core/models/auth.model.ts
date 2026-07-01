export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string | null;
  email: string;
  message: string;
  requiresOtp: boolean;
}

export interface VerifyOtpRequest {
  email: string;
  otpCode: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export interface RegisterResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  message: string;
}
