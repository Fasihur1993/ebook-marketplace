import {
  ApplicationConfig,
  inject,
  provideBrowserGlobalErrorListeners,
  provideAppInitializer,
} from '@angular/core';

import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { provideRouter } from '@angular/router';
import { catchError, of } from 'rxjs';

import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthTokenService } from './core/services/auth-token.service';
import { AuthStateService } from './core/services/auth-state.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    provideHttpClient(withInterceptors([authInterceptor])),

    provideRouter(routes),

    provideAppInitializer(() => {
      const tokenService = inject(AuthTokenService);
      const authState = inject(AuthStateService);

      if (!tokenService.hasToken()) {
        return;
      }

      return authState.loadCurrentUser().pipe(
        catchError(() => {
          authState.logout();
          return of(null);
        }),
      );
    }),
  ],
};
