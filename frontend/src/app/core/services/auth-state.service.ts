
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap, throwError } from 'rxjs';

import { ApiResponse } from '../models/api-response.model';
import {
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../models/auth.model';

import { AuthApiService } from './auth-api.service';
import { AuthTokenService } from './auth-token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthStateService {
  private readonly authApi = inject(AuthApiService);
  private readonly authToken = inject(AuthTokenService);

  readonly currentUser = signal<CurrentUser | null>(null);

  readonly isAuthenticated = computed(
    () => this.currentUser() !== null,
  );

  readonly isAdmin = computed(
    () => this.currentUser()?.role === 'ADMIN',
  );

  register(
    request: RegisterRequest,
  ): Observable<ApiResponse<RegisterResponse>> {
    return this.authApi.register(request);
  }

  login(
    request: LoginRequest,
  ): Observable<ApiResponse<LoginResponse>> {
    return this.authApi.login(request).pipe(
      tap((response) => {
        if (response.success && response.data?.accessToken) {
          this.authToken.setToken(response.data.accessToken);
        }
      }),
    );
  }

  loadCurrentUser(): Observable<ApiResponse<CurrentUser>> {
    return this.authApi.getCurrentUser().pipe(
      tap((response) => {
        if (response.success && response.data) {
          this.currentUser.set(response.data);
        } else {
          this.logout();
        }
      }),
    );
  }

  logout(): void {
    this.authToken.clearToken();
    this.currentUser.set(null);
  }
}
