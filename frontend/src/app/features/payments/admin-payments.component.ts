import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminPayment } from '../../core/models/admin-payment.model';
import { AdminPaymentApiService } from '../../core/services/admin-payment-api.service';



@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-payments.component.html',
  styleUrl: './admin-payments.component.scss',
})
export class AdminPaymentsComponent implements OnInit, OnDestroy {
  private readonly paymentApi = inject(AdminPaymentApiService);

  readonly payments = signal<AdminPayment[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly currentPage = signal(0);
  readonly pageSize = 10;
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly actionInProgress = signal<number | null>(null);

  readonly rejectingPayment = signal<AdminPayment | null>(null);
  readonly rejectionReason = signal('');

  readonly screenshotPayment = signal<AdminPayment | null>(null);
  readonly screenshotUrl = signal<string | null>(null);
  readonly screenshotLoading = signal(false);

  ngOnInit(): void {
    this.loadPayments();
  }

  ngOnDestroy(): void {
    this.clearScreenshotUrl();
  }

  loadPayments(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.paymentApi.getSubmittedPayments(this.currentPage(), this.pageSize).subscribe({
      next: (response:any) => {
        this.payments.set(response.data.content ?? []);
        this.totalPages.set(response.data.totalPages);
        this.totalElements.set(response.data.totalElements);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.getErrorMessage(error, 'Unable to load submitted payments.'));
        this.loading.set(false);
      },
    });
  }

  previousPage(): void {
    if (this.currentPage() <= 0) {
      return;
    }

    this.currentPage.update((page) => page - 1);
    this.loadPayments();
  }

  nextPage(): void {
    if (this.currentPage() >= this.totalPages() - 1) {
      return;
    }

    this.currentPage.update((page) => page + 1);
    this.loadPayments();
  }

  verifyPayment(payment: AdminPayment): void {
    const confirmed = window.confirm(
      `Verify payment #${payment.paymentId} for ${this.formatAmount(payment.amount, payment.currency)}?`,
    );

    if (!confirmed) {
      return;
    }

    this.actionInProgress.set(payment.paymentId);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.paymentApi.verifyPayment(payment.paymentId).subscribe({
      next: (response:any) => {
        this.successMessage.set(
          `Payment #${response.data.paymentId} verified successfully. Order #${response.data.orderId} is now ${response.data.orderStatus}.`,
        );

        this.actionInProgress.set(null);
        this.loadPayments();
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.getErrorMessage(error, 'Unable to verify this payment.'));
        this.actionInProgress.set(null);
      },
    });
  }

  openRejectDialog(payment: AdminPayment): void {
    this.errorMessage.set('');
    this.successMessage.set('');
    this.rejectionReason.set('');
    this.rejectingPayment.set(payment);
  }

  closeRejectDialog(): void {
    if (this.actionInProgress() !== null) {
      return;
    }

    this.rejectingPayment.set(null);
    this.rejectionReason.set('');
  }

  submitRejection(): void {
    const payment = this.rejectingPayment();
    const reason = this.rejectionReason().trim();

    if (!payment || !reason) {
      return;
    }

    this.actionInProgress.set(payment.paymentId);
    this.errorMessage.set('');

    this.paymentApi.rejectPayment(payment.paymentId, { reason }).subscribe({
      next: (response:any) => {
        this.successMessage.set(`Payment #${response.data.paymentId} rejected successfully.`);

        this.rejectingPayment.set(null);
        this.rejectionReason.set('');
        this.actionInProgress.set(null);
        this.loadPayments();
      },
      error: (error: unknown) => {
        this.errorMessage.set(this.getErrorMessage(error, 'Unable to reject this payment.'));
        this.actionInProgress.set(null);
      },
    });
  }

  openScreenshot(payment: AdminPayment): void {
    if (!payment.screenshotAvailable) {
      return;
    }

    this.clearScreenshotUrl();
    this.screenshotPayment.set(payment);
    this.screenshotLoading.set(true);
    this.errorMessage.set('');

    this.paymentApi.getPaymentScreenshot(payment.paymentId).subscribe({
      next: (blob) => {
        this.screenshotUrl.set(URL.createObjectURL(blob));
        this.screenshotLoading.set(false);
      },
      error: (error: unknown) => {
        this.screenshotLoading.set(false);
        this.screenshotPayment.set(null);
        this.errorMessage.set(this.getErrorMessage(error, 'Unable to load payment screenshot.'));
      },
    });
  }

  closeScreenshot(): void {
    this.screenshotPayment.set(null);
    this.clearScreenshotUrl();
  }

  formatAmount(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
    }).format(amount);
  }

  private clearScreenshotUrl(): void {
    const url = this.screenshotUrl();

    if (url) {
      URL.revokeObjectURL(url);
      this.screenshotUrl.set(null);
    }
  }

  private getErrorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const body = (error as { error?: { message?: string } }).error;

      if (body?.message) {
        return body.message;
      }
    }

    return fallback;
  }
}
