import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CustomerLibraryApiService } from '../../../core/services/customer-library-api.service';
import { PurchasedEbook } from '../../../core/models/purchased-ebook.model';
import { StoreHeaderComponent } from '../../../shared/components/store-header/store-header.component';

@Component({
  selector: 'app-my-library',
  standalone: true,
  imports: [CommonModule, RouterLink, StoreHeaderComponent],
  templateUrl: './my-library.component.html',
})
export class MyLibraryComponent implements OnInit {
  private readonly libraryApi = inject(CustomerLibraryApiService);

  readonly ebooks = signal<PurchasedEbook[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly downloadingIds = signal<Set<number>>(new Set());
  readonly downloadErrors = signal<Record<number, string>>({});

  private readonly pageSize = 12;

  ngOnInit(): void {
    this.loadLibrary();
  }

  loadLibrary(page = 0): void {
    this.loading.set(true);
    this.error.set(null);

    this.libraryApi.getLibrary(page, this.pageSize).subscribe({
      next: (response) => {
        if (!response.success || !response.data) {
          this.error.set(response.message || 'Unable to load your library.');
          this.loading.set(false);
          return;
        }

        this.ebooks.set(response.data.content);
        this.currentPage.set(response.data.number);
        this.totalPages.set(response.data.totalPages);
        this.totalElements.set(response.data.totalElements);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(err.error?.message || 'Unable to load your library. Please try again.');
        this.loading.set(false);
      },
    });
  }

  downloadEbook(ebook: PurchasedEbook): void {
    const ebookId = ebook.ebookId;

    this.downloadingIds.update((ids) => {
      const updated = new Set(ids);
      updated.add(ebookId);
      return updated;
    });

    this.clearDownloadError(ebookId);

    this.libraryApi.downloadEbook(ebookId).subscribe({
      next: (response) => {
        const blob = response.body;

        if (!blob) {
          this.setDownloadError(ebookId, 'The downloaded file is empty.');
          this.finishDownload(ebookId);
          return;
        }

        const filename = this.getFilename(response.headers.get('Content-Disposition'), ebookId);

        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = objectUrl;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();

        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);

        this.finishDownload(ebookId);
      },
      error: (err: HttpErrorResponse) => {
        this.setDownloadError(
          ebookId,
          err.status === 403
            ? 'You are not authorized to download this ebook.'
            : err.status === 404
              ? 'The PDF file could not be found.'
              : 'Download failed. Please try again.',
        );

        this.finishDownload(ebookId);
      },
    });
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.loadLibrary(page);
  }

  formatDate(value: string): string {
    return new Intl.DateTimeFormat('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    }).format(new Date(value));
  }

  private getFilename(contentDisposition: string | null, ebookId: number): string {
    const match = contentDisposition?.match(/filename="?([^";]+)"?/i);
    return match?.[1] || `ebook-${ebookId}.pdf`;
  }

  private setDownloadError(ebookId: number, message: string): void {
    this.downloadErrors.update((errors) => ({
      ...errors,
      [ebookId]: message,
    }));
  }

  private clearDownloadError(ebookId: number): void {
    this.downloadErrors.update((errors) => {
      const updated = { ...errors };
      delete updated[ebookId];
      return updated;
    });
  }

  private finishDownload(ebookId: number): void {
    this.downloadingIds.update((ids) => {
      const updated = new Set(ids);
      updated.delete(ebookId);
      return updated;
    });
  }
}
