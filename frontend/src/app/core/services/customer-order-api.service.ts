import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ApiResponse } from '../models/api-response.model';
import { CustomerOrder, SpringPage } from '../models/customer-order.model';

@Injectable({
  providedIn: 'root',
})
export class CustomerOrderApiService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${API_BASE_URL}/me/orders`;

  getOrders(
    page: number = 0,
    size: number = 20,
  ): Observable<ApiResponse<SpringPage<CustomerOrder>>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<ApiResponse<SpringPage<CustomerOrder>>>(this.apiUrl, { params });
  }
}
