import { PaymentMethod, PaymentStatus } from './customer-order.model';

export interface CustomerPaymentActionResponse {
  paymentId: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  amount: number;
  currency: string;
  transactionReference: string | null;
}

export interface SubmitUpiPaymentRequest {
  transactionReference: string;
  screenshot: File;
}
