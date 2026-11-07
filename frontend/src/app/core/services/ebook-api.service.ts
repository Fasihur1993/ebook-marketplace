import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ApiResponse } from '../models/api-response.model';
import { Ebook, EbookListResponse, EbookPreviewImage } from '../models/ebook.model';

@Injectable({
  providedIn: 'root',
})
export class EbookApiService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${API_BASE_URL}/ebooks`;

  getEbooks(
    page: number = 0,
    size: number = 12,
    categoryId?: number,
    search?: string,
  ): Observable<ApiResponse<EbookListResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (categoryId !== undefined) {
      params = params.set('categoryId', categoryId);
    }

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<ApiResponse<EbookListResponse>>(this.apiUrl, { params });
  }

  getEbookBySlug(slug: string): Observable<ApiResponse<Ebook>> {
    return this.http.get<ApiResponse<Ebook>>(`${this.apiUrl}/${encodeURIComponent(slug)}`);
  }

  getEbookCoverUrl(ebookId: number): string {
    return `${this.apiUrl}/${ebookId}/cover`;
  }

  getEbookPreviews(ebookId: number): Observable<ApiResponse<EbookPreviewImage[]>> {
    return this.http.get<ApiResponse<EbookPreviewImage[]>>(`${this.apiUrl}/${ebookId}/previews`);
  }
}
