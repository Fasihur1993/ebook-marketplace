import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';



import {
  AdminPaymentPage,
  AdminPaymentActionResponse,
  RejectPaymentRequest,
} from '../models/admin-payment.model';
import { ApiResponse } from '../models/api-response.model';
import { API_BASE_URL } from '../config/api.config';

@Injectable({
  providedIn: 'root',
})
export class AdminPaymentApiService {
  private readonly http = inject(HttpClient);

  getSubmittedPayments(page = 0, size = 10): Observable<ApiResponse<AdminPaymentPage>> {
    const params = new HttpParams().set('page', page).set('size', size);

    return this.http.get<ApiResponse<AdminPaymentPage>>(
      `${API_BASE_URL}/admin/payments/submitted`,
      { params },
    );
  }

  getPaymentScreenshot(paymentId: number): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/admin/payments/${paymentId}/screenshot`, {
      responseType: 'blob',
    });
  }

  verifyPayment(paymentId: number): Observable<ApiResponse<AdminPaymentActionResponse>> {
    return this.http.post<ApiResponse<AdminPaymentActionResponse>>(
      `${API_BASE_URL}/admin/payments/${paymentId}/verify`,
      {},
    );
  }

  rejectPayment(
    paymentId: number,
    request: RejectPaymentRequest,
  ): Observable<ApiResponse<AdminPaymentActionResponse>> {
    return this.http.post<ApiResponse<AdminPaymentActionResponse>>(
      `${API_BASE_URL}/admin/payments/${paymentId}/reject`,
      request,
    );
  }
}
