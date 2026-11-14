export type OrderStatus =
  'PENDING_PAYMENT' | 'PAYMENT_SUBMITTED' | 'PAID' | 'CANCELLED' | 'EXPIRED';

export type PaymentStatus = 'PENDING' | 'SUBMITTED' | 'VERIFIED' | 'REJECTED' | 'FAILED';

export type PaymentMethod = 'UPI_MANUAL' | 'RAZORPAY';

export interface CustomerOrderItem {
  ebookId: number;
  ebookTitle: string;
  authorId: number | null;
  unitPrice: number;
  currency: string;
  quantity: number;
}

export interface CustomerPaymentAttempt {
  paymentId: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  amount: number;
  currency: string;
  transactionReference: string | null;
  rejectionReason: string | null;
  createdAt: string;
  submittedAt: string | null;
  verifiedAt: string | null;
}

export interface CustomerOrder {
  orderId: number;
  totalAmount: number;
  currency: string;
  status: OrderStatus;
  items: CustomerOrderItem[];
  latestPayment: CustomerPaymentAttempt | null;
  createdAt: string;
  paidAt: string | null;
}

/**
 * Payment returned immediately when a new order is created.
 */
export interface CreateOrderPaymentResponse {
  paymentId: number;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  amount: number;
  currency: string;
  transactionReference: string | null;
  submittedAt: string | null;
}

/**
 * Response from POST /api/v1/orders.
 */
export interface CreateOrderResponse {
  orderId: number;
  totalAmount: number;
  currency: string;
  status: OrderStatus;
  items: CustomerOrderItem[];
  payment: CreateOrderPaymentResponse;
  createdAt: string;
}

export interface SpringPage<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
