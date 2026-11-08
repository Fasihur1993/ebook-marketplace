import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ApiResponse } from '../models/api-response.model';
import { CustomerPaymentActionResponse } from '../models/customer-payment.model';

@Injectable({
  providedIn: 'root',
})
export class CustomerPaymentApiService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${API_BASE_URL}/orders`;

  retryPayment(orderId: number): Observable<ApiResponse<CustomerPaymentActionResponse>> {
    return this.http.post<ApiResponse<CustomerPaymentActionResponse>>(
      `${this.apiUrl}/${orderId}/payments/retry`,
      null,
    );
  }

  submitUpiPayment(
    orderId: number,
    paymentId: number,
    transactionReference: string,
    screenshot: File,
  ): Observable<ApiResponse<CustomerPaymentActionResponse>> {
    const formData = new FormData();

    formData.append('transactionReference', transactionReference);
    formData.append('screenshot', screenshot);

    return this.http.post<ApiResponse<CustomerPaymentActionResponse>>(
      `${this.apiUrl}/${orderId}/payments/${paymentId}/upi`,
      formData,
    );
  }
}
