import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { catchError, EMPTY, of, switchMap } from 'rxjs';

import { EbookApiService } from '../../core/services/ebook-api.service';
import { Ebook, EbookPreviewImage } from '../../core/models/ebook.model';
import { StoreHeaderComponent } from '../../shared/components/store-header/store-header.component';

@Component({
  selector: 'app-ebook-details',
  standalone: true,
  imports: [CommonModule, RouterLink, StoreHeaderComponent],
  templateUrl: './ebook-details.html',
  styleUrl: './ebook-details.scss',
})
export class EbookDetailsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly ebookApi = inject(EbookApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly ebook = signal<Ebook | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly coverLoading = signal(true);
  readonly coverError = signal(false);

  readonly previews = signal<EbookPreviewImage[]>([]);
  readonly previewsLoading = signal(false);
  readonly previewsError = signal(false);

  readonly selectedPreview = signal<EbookPreviewImage | null>(null);

  readonly coverUrl = computed(() => {
    const book = this.ebook();

    return book ? this.ebookApi.getEbookCoverUrl(book.id) : '';
  });

  ngOnInit(): void {
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const slug = params.get('slug');

          if (!slug) {
            this.error.set('Invalid ebook URL.');
            this.loading.set(false);
            return EMPTY;
          }

          this.loading.set(true);
          this.error.set('');
          this.ebook.set(null);
          this.previews.set([]);
          this.selectedPreview.set(null);
          this.coverLoading.set(true);
          this.coverError.set(false);
          this.previewsError.set(false);

          return this.ebookApi.getEbookBySlug(slug).pipe(
            catchError((error) => {
              console.error('Failed to load ebook:', error);
              this.error.set('Unable to load ebook details. Please try again.');
              this.loading.set(false);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        if (!response.success || !response.data) {
          this.error.set(response.message || 'Ebook not found.');
          this.loading.set(false);
          return;
        }

        const book = response.data;

        this.ebook.set(book);
        this.loading.set(false);

        this.loadPreviews(book.id);
      });
  }

  private loadPreviews(ebookId: number): void {
    this.previewsLoading.set(true);
    this.previewsError.set(false);

    this.ebookApi
      .getEbookPreviews(ebookId)
      .pipe(
        catchError((error) => {
          console.error('Failed to load ebook previews:', error);
          this.previewsError.set(true);
          this.previewsLoading.set(false);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((response) => {
        if (!response) {
          return;
        }

        if (!response.success || !response.data) {
          this.previewsError.set(true);
          this.previewsLoading.set(false);
          return;
        }

        this.previews.set(response.data);
        this.previewsLoading.set(false);
      });
  }

  openPreview(preview: EbookPreviewImage): void {
    this.selectedPreview.set(preview);
  }

  closePreview(): void {
    this.selectedPreview.set(null);
  }

  formatPrice(price: number, currency: string): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(price);
  }
}
