import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthTokenService {
  private readonly tokenKey = 'ebook_marketplace_access_token';

  getToken(): string | null {
    return sessionStorage.getItem(this.tokenKey);
  }

  setToken(token: string): void {
    sessionStorage.setItem(this.tokenKey, token);
  }

  clearToken(): void {
    sessionStorage.removeItem(this.tokenKey);
  }

  hasToken(): boolean {
    return this.getToken() !== null;
  }
}