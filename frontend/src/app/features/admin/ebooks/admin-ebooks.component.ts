import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Ebook, EbookPreviewImage } from '../../../core/models/ebook.model';
import { AdminEbookApiService } from '../services/admin-ebook-api.service';

@Component({
  selector: 'app-admin-ebooks',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-ebooks.component.html',
  styleUrl: './admin-ebooks.component.scss',
})
export class AdminEbooksComponent implements OnInit, OnDestroy {
  private readonly ebookApi = inject(AdminEbookApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly ebooks = signal<Ebook[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly currentPage = signal(0);
  readonly pageSize = 10;
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly uploadingEbookId = signal<number | null>(null);
  readonly uploadError = signal('');
  readonly uploadSuccess = signal('');

  readonly publishingEbookId = signal<number | null>(null);
  readonly publishError = signal('');
  readonly publishSuccess = signal('');

  readonly unpublishingEbookId = signal<number | null>(null);
  readonly unpublishError = signal('');
  readonly unpublishSuccess = signal('');
  readonly ebookToUnpublish = signal<Ebook | null>(null);

  readonly ebookToPublish = signal<Ebook | null>(null);

  readonly suspendingEbookId = signal<number | null>(null);
  readonly suspendError = signal('');
  readonly suspendSuccess = signal('');
  readonly ebookToSuspend = signal<Ebook | null>(null);
  readonly suspensionReason = signal('');

  readonly restoringEbookId = signal<number | null>(null);
  readonly restoreError = signal('');
  readonly restoreSuccess = signal('');
  readonly ebookToRestore = signal<Ebook | null>(null);

  // PDF review state
  readonly ebookToReview = signal<Ebook | null>(null);
  readonly reviewLoading = signal(false);
  readonly reviewError = signal('');
  readonly reviewCoverUrl = signal<string | null>(null);
  readonly reviewPreviewImages = signal<{ pageNumber: number; imageUrl: string }[]>([]);
  readonly reviewedEbookIds = signal<Set<number>>(new Set());

  private readonly reviewObjectUrls = new Set<string>();

  ngOnInit(): void {
    this.loadEbooks();
  }

  ngOnDestroy(): void {
    this.releaseReviewObjectUrls();
  }

  loadEbooks(page: number = 0): void {
    this.loading.set(true);
    this.error.set('');

    this.ebookApi.getEbooks(page, this.pageSize).subscribe({
      next: (response) => {
        if (!response.success || !response.data) {
          this.error.set(response.message || 'Unable to load ebooks.');
          this.ebooks.set([]);
          this.loading.set(false);
          return;
        }

        this.ebooks.set(response.data.ebooks);
        this.currentPage.set(response.data.page);
        this.totalPages.set(response.data.totalPages);
        this.totalElements.set(response.data.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Failed to load admin ebooks:', error);

        this.error.set('Unable to load ebooks. Please try again.');
        this.ebooks.set([]);
        this.loading.set(false);
      },
    });
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.loadEbooks(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() + 1 < this.totalPages()) {
      this.loadEbooks(this.currentPage() + 1);
    }
  }

  formatPrice(price: number, currency: string): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(price);
  }

  onPdfSelected(event: Event, ebook: Ebook): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    this.uploadError.set('');
    this.uploadSuccess.set('');

    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

    if (!isPdf) {
      this.uploadError.set('Please select a valid PDF file.');
      input.value = '';
      return;
    }

    this.uploadingEbookId.set(ebook.id);

    this.ebookApi
      .uploadPdf(ebook.id, file)
      .pipe(
        finalize(() => {
          this.uploadingEbookId.set(null);
          input.value = '';
        }),
      )
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.ebooks.update((ebooks) =>
              ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
            );

            this.uploadSuccess.set(`PDF uploaded successfully for "${ebook.title}".`);
          } else {
            this.uploadError.set(response.message || 'PDF upload failed.');
          }
        },
        error: (err) => {
          this.uploadError.set(err.error?.message || 'Unable to upload PDF. Please try again.');
        },
      });
  }

  // PDF review
  reviewPdf(ebook: Ebook): void {
    if (!ebook.pdfUploaded) {
      return;
    }

    this.closePdfReview();

    this.ebookToReview.set(ebook);
    this.reviewLoading.set(true);
    this.reviewError.set('');

    forkJoin({
      cover: this.ebookApi.getCoverImage(ebook.id),
      previews: this.ebookApi.getPreviewImages(ebook.id),
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.reviewLoading.set(false)),
      )
      .subscribe({
        next: ({ cover, previews }) => {
          if (!previews.success || !previews.data) {
            this.reviewError.set(previews.message || 'Unable to load PDF previews.');
            return;
          }

          this.reviewCoverUrl.set(this.createObjectUrl(cover));

          const sortedPreviews = [...previews.data].sort((a, b) => a.pageNumber - b.pageNumber);

          const previewRequests = sortedPreviews.map((preview) =>
            this.ebookApi.getPreviewImage(ebook.id, preview.pageNumber),
          );

          if (previewRequests.length === 0) {
            this.reviewError.set('No preview pages were generated.');
            return;
          }

          forkJoin(previewRequests)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (images) => {
                this.reviewPreviewImages.set(
                  images.map((image, index) => ({
                    pageNumber: sortedPreviews[index].pageNumber,
                    imageUrl: this.createObjectUrl(image),
                  })),
                );
                this.reviewedEbookIds.update((ids) => new Set(ids).add(ebook.id));
              },
              error: () => {
                this.reviewError.set('Unable to load one or more PDF preview pages.');
              },
            });
        },
        error: () => {
          this.reviewError.set('Unable to load the PDF review. Please try again.');
        },
      });
  }

  closePdfReview(): void {
    this.releaseReviewObjectUrls();

    this.ebookToReview.set(null);
    this.reviewCoverUrl.set(null);
    this.reviewPreviewImages.set([]);
    this.reviewError.set('');
    this.reviewLoading.set(false);
  }

  private createObjectUrl(blob: Blob): string {
    const url = URL.createObjectURL(blob);
    this.reviewObjectUrls.add(url);
    return url;
  }

  private releaseReviewObjectUrls(): void {
    this.reviewObjectUrls.forEach((url) => URL.revokeObjectURL(url));
    this.reviewObjectUrls.clear();
  }

  confirmPublish(ebook: Ebook): void {
    if (
      !ebook.pdfUploaded ||
      ebook.status === 'PUBLISHED' ||
      !this.reviewedEbookIds().has(ebook.id)
    ) {
      return;
    }

    this.publishError.set('');
    this.publishSuccess.set('');
    this.ebookToPublish.set(ebook);
  }

  cancelPublish(): void {
    if (this.publishingEbookId() !== null) {
      return;
    }

    this.ebookToPublish.set(null);
  }

  publishEbook(): void {
    const ebook = this.ebookToPublish();

    if (
      !ebook ||
      !ebook.pdfUploaded ||
      !this.reviewedEbookIds().has(ebook.id) ||
      this.publishingEbookId() !== null
    ) {
      return;
    }

    this.publishError.set('');
    this.publishSuccess.set('');
    this.publishingEbookId.set(ebook.id);

    this.ebookApi
      .publishEbook(ebook.id)
      .pipe(finalize(() => this.publishingEbookId.set(null)))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.ebooks.update((ebooks) =>
              ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
            );

            this.publishSuccess.set(`"${ebook.title}" has been published successfully.`);

            this.ebookToPublish.set(null);
          } else {
            this.publishError.set(response.message || 'Unable to publish ebook.');
          }
        },
        error: (err) => {
          this.publishError.set(err.error?.message || 'Unable to publish ebook.');
        },
      });
  }

  confirmUnpublish(ebook: Ebook): void {
    if (ebook.status !== 'PUBLISHED') {
      return;
    }

    this.unpublishError.set('');
    this.ebookToUnpublish.set(ebook);
  }

  cancelUnpublish(): void {
    if (this.unpublishingEbookId() !== null) {
      return;
    }

    this.ebookToUnpublish.set(null);
    this.unpublishError.set('');
  }

  unpublishEbook(): void {
    const ebook = this.ebookToUnpublish();

    if (!ebook) {
      return;
    }

    this.unpublishingEbookId.set(ebook.id);
    this.unpublishError.set('');
    this.unpublishSuccess.set('');

    this.ebookApi
      .unpublishEbook(ebook.id)
      .pipe(finalize(() => this.unpublishingEbookId.set(null)))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.ebooks.update((ebooks) =>
              ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
            );

            this.unpublishSuccess.set(`${ebook.title} has been unpublished successfully.`);
          } else {
            this.unpublishError.set(response.message || 'Unable to unpublish ebook.');
          }

          this.ebookToUnpublish.set(null);
        },
        error: (error) => {
          this.unpublishError.set(
            error.error?.message ?? 'Unable to unpublish ebook. Please try again.',
          );
        },
      });
  }

  confirmSuspend(ebook: Ebook): void {
    if (ebook.status !== 'PUBLISHED' && ebook.status !== 'UNPUBLISHED') {
      return;
    }

    this.suspendError.set('');
    this.suspendSuccess.set('');
    this.suspensionReason.set('');
    this.ebookToSuspend.set(ebook);
  }

  cancelSuspend(): void {
    if (this.suspendingEbookId() !== null) {
      return;
    }

    this.ebookToSuspend.set(null);
    this.suspensionReason.set('');
    this.suspendError.set('');
  }

  suspendEbook(): void {
    const ebook = this.ebookToSuspend();
    const reason = this.suspensionReason().trim();

    if (!ebook || !reason || this.suspendingEbookId() !== null) {
      return;
    }

    this.suspendError.set('');
    this.suspendSuccess.set('');
    this.suspendingEbookId.set(ebook.id);

    this.ebookApi
      .suspendEbook(ebook.id, reason)
      .pipe(finalize(() => this.suspendingEbookId.set(null)))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.ebooks.update((ebooks) =>
              ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
            );

            this.suspendSuccess.set(`"${ebook.title}" has been suspended successfully.`);
            this.ebookToSuspend.set(null);
            this.suspensionReason.set('');
          } else {
            this.suspendError.set(response.message || 'Unable to suspend ebook.');
          }
        },
        error: (error) => {
          this.suspendError.set(
            error.error?.message || 'Unable to suspend ebook. Please try again.',
          );
        },
      });
  }

  confirmRestore(ebook: Ebook): void {
    if (ebook.status !== 'SUSPENDED') {
      return;
    }

    this.restoreError.set('');
    this.restoreSuccess.set('');
    this.ebookToRestore.set(ebook);
  }

  cancelRestore(): void {
    if (this.restoringEbookId() !== null) {
      return;
    }

    this.ebookToRestore.set(null);
    this.restoreError.set('');
  }

  restoreEbook(): void {
    const ebook = this.ebookToRestore();

    if (!ebook || this.restoringEbookId() !== null) {
      return;
    }

    this.restoreError.set('');
    this.restoreSuccess.set('');
    this.restoringEbookId.set(ebook.id);

    this.ebookApi
      .restoreEbook(ebook.id)
      .pipe(finalize(() => this.restoringEbookId.set(null)))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.ebooks.update((ebooks) =>
              ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
            );

            this.restoreSuccess.set(`"${ebook.title}" has been restored successfully.`);
            this.ebookToRestore.set(null);
          } else {
            this.restoreError.set(response.message || 'Unable to restore ebook.');
          }
        },
        error: (error) => {
          this.restoreError.set(
            error.error?.message || 'Unable to restore ebook. Please try again.',
          );
        },
      });
  }
}
