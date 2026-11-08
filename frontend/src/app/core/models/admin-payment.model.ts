export type AdminPaymentStatus = 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'FAILED';

export interface AdminPaymentItem {
  ebookId: number;
  ebookTitle: string;
  authorId: number | null;
  unitPrice: number;
  currency: string;
  quantity: number;
}

export interface AdminPayment {
  paymentId: number;
  orderId: number;
  customerName: string;
  customerEmail: string;
  paymentMethod: string;
  status: AdminPaymentStatus;
  amount: number;
  currency: string;
  transactionReference: string | null;
  submittedAt: string | null;
  screenshotAvailable: boolean;
  items: AdminPaymentItem[];
}

export interface AdminPaymentPage {
  content: AdminPayment[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  numberOfElements: number;
}

export interface AdminPaymentActionResponse {
  paymentId: number;
  paymentStatus: AdminPaymentStatus;
  orderId: number;
  orderStatus: string;
  verifiedAt: string | null;
  rejectionReason: string | null;
}

export interface RejectPaymentRequest {
  reason: string;
}
