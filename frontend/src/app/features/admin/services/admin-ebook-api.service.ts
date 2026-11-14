import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Ebook, EbookListResponse } from '../../../core/models/ebook.model';
import { EbookPreviewImage } from '../../../core/models/ebook.model';

export interface CreateEbookRequest {
  title: string;
  description: string;
  price: number;
  categoryId: number;
  pageCount: number | null;
  language: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminEbookApiService {
  private readonly http = inject(HttpClient);

  getEbooks(page = 0, size = 10): Observable<ApiResponse<EbookListResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<ApiResponse<EbookListResponse>>(`${API_BASE_URL}/admin/ebooks`, {
      params,
    });
  }

  getCategories(): Observable<ApiResponse<Category[]>> {
    return this.http.get<ApiResponse<Category[]>>(`${API_BASE_URL}/admin/categories`);
  }

  createEbook(request: CreateEbookRequest): Observable<ApiResponse<unknown>> {
    return this.http.post<ApiResponse<unknown>>(`${API_BASE_URL}/admin/ebooks`, request);
  }

  uploadPdf(ebookId: number, file: File): Observable<ApiResponse<Ebook>> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<Ebook>>(
      `${API_BASE_URL}/admin/ebooks/${ebookId}/pdf`,
      formData,
    );
  }

  publishEbook(ebookId: number): Observable<ApiResponse<Ebook>> {
    return this.http.post<ApiResponse<Ebook>>(
      `${API_BASE_URL}/admin/ebooks/${ebookId}/publish`,
      {},
    );
  }

  unpublishEbook(ebookId: number): Observable<ApiResponse<Ebook>> {
    return this.http.post<ApiResponse<Ebook>>(
      `${API_BASE_URL}/admin/ebooks/${ebookId}/unpublish`,
      {},
    );
  }

  suspendEbook(ebookId: number, reason: string): Observable<ApiResponse<Ebook>> {
    return this.http.post<ApiResponse<Ebook>>(`${API_BASE_URL}/admin/ebooks/${ebookId}/suspend`, {
      reason,
    });
  }

  restoreEbook(ebookId: number): Observable<ApiResponse<Ebook>> {
    return this.http.post<ApiResponse<Ebook>>(
      `${API_BASE_URL}/admin/ebooks/${ebookId}/restore`,
      {},
    );
  }

  getCoverImage(ebookId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/admin/ebooks/${ebookId}/cover`, { responseType: 'blob' });
  }

  getPreviewImages(ebookId: number): Observable<ApiResponse<EbookPreviewImage[]>> {
    return this.http.get<ApiResponse<EbookPreviewImage[]>>(
      `${API_BASE_URL}/admin/ebooks/${ebookId}/previews`,
    );
  }

  getPreviewImage(ebookId: number, pageNumber: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/admin/ebooks/${ebookId}/previews/${pageNumber}`, {
      responseType: 'blob',
    });
  }
}