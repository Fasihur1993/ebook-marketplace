import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/api.config';
import { ApiResponse } from '../models/api-response.model';

import {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class AuthApiService {
  private readonly http = inject(HttpClient);

  private readonly authUrl = `${API_BASE_URL}/auth`;

  register(
    request: RegisterRequest,
  ): Observable<ApiResponse<RegisterResponse>> {
    return this.http.post<ApiResponse<RegisterResponse>>(
      `${this.authUrl}/register`,
      request,
    );
  }

  login(
    request: LoginRequest,
  ): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(
      `${this.authUrl}/login`,
      request,
    );
  }

  getCurrentUser(): Observable<ApiResponse<CurrentUser>> {
    return this.http.get<ApiResponse<CurrentUser>>(
      `${this.authUrl}/me`,
    );
  }
}