import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ApiResponse } from '../models/api-response.model';
import { PurchasedEbook } from '../models/purchased-ebook.model';
import { SpringPage } from '../models/customer-order.model';

@Injectable({
  providedIn: 'root',
})
export class CustomerLibraryApiService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${API_BASE_URL}/me/library`;

  getLibrary(
    page: number = 0,
    size: number = 20,
  ): Observable<ApiResponse<SpringPage<PurchasedEbook>>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<ApiResponse<SpringPage<PurchasedEbook>>>(this.apiUrl, { params });
  }

  downloadEbook(ebookId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.apiUrl}/${ebookId}/download`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}
