import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CustomerOrderApiService } from '../../../core/services/customer-order-api.service';
import { CustomerPaymentApiService } from '../../../core/services/customer-payment-api.service';

import {
  CustomerOrder,
  OrderStatus,
  PaymentStatus,
  SpringPage,
} from '../../../core/models/customer-order.model';

@Component({
  selector: 'app-customer-orders',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './customer-orders.component.html',
})
export class CustomerOrdersComponent implements OnInit {
  private readonly orderApi = inject(CustomerOrderApiService);
  private readonly paymentApi = inject(CustomerPaymentApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly orders = signal<CustomerOrder[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  // UPI submission form state, maintained separately for each order.
  readonly transactionReferences = signal<Record<number, string>>({});
  readonly selectedScreenshots = signal<Record<number, File | null>>({});
  readonly fileErrors = signal<Record<number, string>>({});
  readonly submissionErrors = signal<Record<number, string>>({});
  readonly submissionSuccess = signal<Record<number, string>>({});

  // Track operations independently for each order.
  readonly submittingOrders = signal<Set<number>>(new Set());
  readonly retryingOrders = signal<Set<number>>(new Set());

  readonly maxScreenshotSize = 5 * 1024 * 1024;

  private readonly pageSize = 10;

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(page: number = 0): void {
    this.loading.set(true);
    this.error.set(null);

    this.orderApi
      .getOrders(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const result = response.data as SpringPage<CustomerOrder>;

          this.orders.set(result.content);
          this.currentPage.set(result.number);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);

          this.loading.set(false);
        },
        error: (httpError: HttpErrorResponse) => {
          this.error.set(
            this.getErrorMessage(httpError, 'Unable to load your orders. Please try again.'),
          );

          this.loading.set(false);
        },
      });
  }

  // --------------------------------------------------
  // UPI submission form
  // --------------------------------------------------

  onTransactionReferenceChange(orderId: number, value: string): void {
    this.transactionReferences.update((references) => ({
      ...references,
      [orderId]: value,
    }));

    this.clearSubmissionFeedback(orderId);
  }

  onScreenshotSelected(orderId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.fileErrors.update((errors) => ({
      ...errors,
      [orderId]: '',
    }));

    this.selectedScreenshots.update((files) => ({
      ...files,
      [orderId]: null,
    }));

    this.clearSubmissionFeedback(orderId);

    if (!file) {
      return;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();

    const allowedExtensions = ['png', 'jpg', 'jpeg'];
    const allowedMimeTypes = ['image/png', 'image/jpeg'];

    if (!extension || !allowedExtensions.includes(extension)) {
      this.setFileError(orderId, 'Only PNG, JPG and JPEG screenshots are supported.');

      input.value = '';
      return;
    }

    if (file.type && !allowedMimeTypes.includes(file.type)) {
      this.setFileError(orderId, 'Please select a valid PNG or JPEG image.');

      input.value = '';
      return;
    }

    if (file.size > this.maxScreenshotSize) {
      this.setFileError(orderId, 'Screenshot size must not exceed 5 MB.');

      input.value = '';
      return;
    }

    if (file.size === 0) {
      this.setFileError(orderId, 'The selected screenshot is empty.');

      input.value = '';
      return;
    }

    this.selectedScreenshots.update((files) => ({
      ...files,
      [orderId]: file,
    }));
  }

  transactionReferenceFor(orderId: number): string {
    return this.transactionReferences()[orderId] ?? '';
  }

  selectedScreenshotFor(orderId: number): File | null {
    return this.selectedScreenshots()[orderId] ?? null;
  }

  fileErrorFor(orderId: number): string | null {
    return this.fileErrors()[orderId] || null;
  }

  submissionErrorFor(orderId: number): string | null {
    return this.submissionErrors()[orderId] || null;
  }

  submissionSuccessFor(orderId: number): string | null {
    return this.submissionSuccess()[orderId] || null;
  }

  isSubmitting(orderId: number): boolean {
    return this.submittingOrders().has(orderId);
  }

  isRetrying(orderId: number): boolean {
    return this.retryingOrders().has(orderId);
  }

  canSubmitUpiPayment(order: CustomerOrder): boolean {
    return (
      order.status === 'PENDING_PAYMENT' &&
      order.latestPayment?.paymentMethod === 'UPI_MANUAL' &&
      order.latestPayment.status === 'PENDING'
    );
  }

  submitUpiPayment(order: CustomerOrder): void {
    const orderId = order.orderId;
    const payment = order.latestPayment;

    // Prevent duplicate submissions.
    if (
      !this.canSubmitUpiPayment(order) ||
      this.isSubmitting(orderId) ||
      this.isRetrying(orderId)
    ) {
      return;
    }

    const transactionReference = this.transactionReferenceFor(orderId).trim();

    const screenshot = this.selectedScreenshotFor(orderId);

    if (!transactionReference) {
      this.setSubmissionError(orderId, 'Please enter your UPI transaction ID / UTR.');
      return;
    }

    if (!screenshot) {
      this.setSubmissionError(orderId, 'Please select your payment screenshot.');
      return;
    }

    if (this.fileErrorFor(orderId)) {
      return;
    }

    this.clearSubmissionFeedback(orderId);
    this.addToSet(this.submittingOrders, orderId);

    this.paymentApi
      .submitUpiPayment(orderId, payment!.paymentId, transactionReference, screenshot)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.submissionSuccess.update((messages) => ({
            ...messages,
            [orderId]: 'Payment details submitted successfully.',
          }));

          this.transactionReferences.update((references) => ({
            ...references,
            [orderId]: '',
          }));

          this.selectedScreenshots.update((files) => ({
            ...files,
            [orderId]: null,
          }));

          this.removeFromSet(this.submittingOrders, orderId);

          // Refresh orders to show the latest payment status.
          this.loadOrders(this.currentPage());
        },
        error: (httpError: HttpErrorResponse) => {
          this.setSubmissionError(
            orderId,
            this.getErrorMessage(httpError, 'Unable to submit payment details. Please try again.'),
          );

          this.removeFromSet(this.submittingOrders, orderId);
        },
      });
  }

  // --------------------------------------------------
  // Retry rejected payments
  // --------------------------------------------------

  canRetryPayment(order: CustomerOrder): boolean {
    return (
      order.status === 'PENDING_PAYMENT' &&
      order.latestPayment?.paymentMethod === 'UPI_MANUAL' &&
      order.latestPayment.status === 'REJECTED'
    );
  }

  retryPayment(order: CustomerOrder): void {
    const orderId = order.orderId;

    // A rejected attempt can be retried only once per request.
    if (!this.canRetryPayment(order) || this.isRetrying(orderId) || this.isSubmitting(orderId)) {
      return;
    }

    this.clearSubmissionFeedback(orderId);
    this.addToSet(this.retryingOrders, orderId);

    this.paymentApi
      .retryPayment(orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          // Clear the previous attempt's form data.
          this.transactionReferences.update((references) => ({
            ...references,
            [orderId]: '',
          }));

          this.selectedScreenshots.update((files) => ({
            ...files,
            [orderId]: null,
          }));

          this.removeFromSet(this.retryingOrders, orderId);

          // The refreshed order contains the newly created PENDING attempt.
          this.loadOrders(this.currentPage());
        },
        error: (httpError: HttpErrorResponse) => {
          this.setSubmissionError(
            orderId,
            this.getErrorMessage(httpError, 'Unable to retry payment. Please try again.'),
          );

          this.removeFromSet(this.retryingOrders, orderId);
        },
      });
  }

  // --------------------------------------------------
  // Form feedback helpers
  // --------------------------------------------------

  private setFileError(orderId: number, message: string): void {
    this.fileErrors.update((errors) => ({
      ...errors,
      [orderId]: message,
    }));
  }

  private setSubmissionError(orderId: number, message: string): void {
    this.submissionErrors.update((errors) => ({
      ...errors,
      [orderId]: message,
    }));
  }

  private clearSubmissionFeedback(orderId: number): void {
    this.submissionErrors.update((errors) => ({
      ...errors,
      [orderId]: '',
    }));

    this.submissionSuccess.update((messages) => ({
      ...messages,
      [orderId]: '',
    }));
  }

  private addToSet(target: ReturnType<typeof signal<Set<number>>>, orderId: number): void {
    target.update((ids) => new Set([...ids, orderId]));
  }

  private removeFromSet(target: ReturnType<typeof signal<Set<number>>>, orderId: number): void {
    target.update((ids) => {
      const updated = new Set(ids);
      updated.delete(orderId);
      return updated;
    });
  }

  private getErrorMessage(httpError: HttpErrorResponse, fallback: string): string {
    const message = httpError.error?.message;

    return typeof message === 'string' ? message : fallback;
  }

  // --------------------------------------------------
  // Pagination
  // --------------------------------------------------

  previousPage(): void {
    if (this.currentPage() > 0 && !this.loading()) {
      this.loadOrders(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() + 1 < this.totalPages() && !this.loading()) {
      this.loadOrders(this.currentPage() + 1);
    }
  }

  retry(): void {
    this.loadOrders(this.currentPage());
  }

  // --------------------------------------------------
  // Status styling
  // --------------------------------------------------

  formatStatus(status: string): string {
    return status.replaceAll('_', ' ');
  }

  orderStatusClass(status: OrderStatus): string {
    switch (status) {
      case 'PAID':
        return 'bg-green-50 text-green-700 ring-green-200';

      case 'PAYMENT_SUBMITTED':
        return 'bg-blue-50 text-blue-700 ring-blue-200';

      case 'PENDING_PAYMENT':
        return 'bg-amber-50 text-amber-700 ring-amber-200';

      case 'CANCELLED':
      case 'EXPIRED':
        return 'bg-gray-100 text-gray-600 ring-gray-200';

      default:
        return 'bg-gray-100 text-gray-600 ring-gray-200';
    }
  }

  paymentStatusClass(status: PaymentStatus): string {
    switch (status) {
      case 'VERIFIED':
        return 'bg-green-50 text-green-700 ring-green-200';

      case 'SUBMITTED':
        return 'bg-blue-50 text-blue-700 ring-blue-200';

      case 'REJECTED':
      case 'FAILED':
        return 'bg-red-50 text-red-700 ring-red-200';

      case 'PENDING':
        return 'bg-amber-50 text-amber-700 ring-amber-200';

      default:
        return 'bg-gray-100 text-gray-600 ring-gray-200';
    }
  }
}
