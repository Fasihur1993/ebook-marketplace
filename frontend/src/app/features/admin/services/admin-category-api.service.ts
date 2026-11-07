import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/config/api.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Category } from '../../../core/models/category.model';

export interface SaveCategoryRequest {
  name: string;
  description: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminCategoryApiService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<ApiResponse<Category[]>> {
    return this.http.get<ApiResponse<Category[]>>(`${API_BASE_URL}/admin/categories`);
  }

  createCategory(request: SaveCategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.post<ApiResponse<Category>>(`${API_BASE_URL}/admin/categories`, request);
  }

  updateCategory(id: number, request: SaveCategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.put<ApiResponse<Category>>(`${API_BASE_URL}/admin/categories/${id}`, request);
  }

  updateCategoryStatus(id: number, active: boolean): Observable<ApiResponse<Category>> {
    const params = new HttpParams().set('active', active);

    return this.http.patch<ApiResponse<Category>>(
      `${API_BASE_URL}/admin/categories/${id}/status`,
      null,
      { params },
    );
  }
}
