import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

import { Ebook } from '../../../core/models/ebook.model';
import { AdminEbookApiService } from '../services/admin-ebook-api.service';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-admin-ebooks',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-ebooks.component.html',
  styleUrl: './admin-ebooks.component.scss',
})
export class AdminEbooksComponent implements OnInit {
  private readonly ebookApi = inject(AdminEbookApiService);

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

  ngOnInit(): void {
    this.loadEbooks();
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

  confirmPublish(ebook: Ebook): void {
    if (!ebook.pdfUploaded || ebook.status === 'PUBLISHED') {
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

    if (!ebook || !ebook.pdfUploaded || this.publishingEbookId() !== null) {
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

    this.ebookApi.unpublishEbook(ebook.id).subscribe({
      next: (response) => {
        if (response.data) {
          this.ebooks.update((ebooks) =>
            ebooks.map((item) => (item.id === ebook.id ? response.data! : item)),
          );
        }

        this.unpublishingEbookId.set(null);
        this.ebookToUnpublish.set(null);
        this.unpublishSuccess.set(`${ebook.title} has been unpublished successfully.`);
      },
      error: (error) => {
        this.unpublishingEbookId.set(null);
        this.unpublishError.set(
          error.error?.message ?? 'Unable to unpublish ebook. Please try again.',
        );
      },
    });
  }
}
