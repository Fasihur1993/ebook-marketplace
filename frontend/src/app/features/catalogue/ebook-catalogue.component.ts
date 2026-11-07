import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { EbookApiService } from '../../core/services/ebook-api.service';
import { Ebook } from '../../core/models/ebook.model';
import { StoreHeaderComponent } from '../../shared/components/store-header/store-header.component';

@Component({
  selector: 'app-ebook-catalogue',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, StoreHeaderComponent],
  templateUrl: './ebook-catalogue.component.html',
  styleUrl: './ebook-catalogue.component.scss',
})
export class EbookCatalogueComponent implements OnInit {
  private readonly ebookApi = inject(EbookApiService);

  readonly ebooks = signal<Ebook[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');

  readonly currentPage = signal(0);
  readonly pageSize = 12;
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly searchTerm = signal('');

  // Tracks ebooks whose cover image could not be loaded.
  readonly failedCoverIds = signal<Set<number>>(new Set());

  ngOnInit(): void {
    this.loadEbooks();
  }

  loadEbooks(page: number = 0): void {
    this.loading.set(true);
    this.error.set('');
    this.failedCoverIds.set(new Set());

    this.ebookApi
      .getEbooks(page, this.pageSize, undefined, this.searchTerm().trim() || undefined)
      .subscribe({
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
          console.error('Failed to load ebooks:', error);

          this.error.set('Unable to connect to the bookstore. Please try again.');

          this.ebooks.set([]);
          this.loading.set(false);
        },
      });
  }

  ebookCoverUrl(ebookId: number): string {
    return this.ebookApi.getEbookCoverUrl(ebookId);
  }

  onCoverError(ebookId: number): void {
    this.failedCoverIds.update((current) => {
      const updated = new Set(current);
      updated.add(ebookId);
      return updated;
    });
  }

  search(): void {
    this.loadEbooks(0);
  }

  clearSearch(): void {
    this.searchTerm.set('');
    this.loadEbooks(0);
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
}
