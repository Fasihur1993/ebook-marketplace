import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(false);
  readonly error = signal('');

  readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.loginForm.invalid || this.loading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authState
      .login(this.loginForm.getRawValue())
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (!response.success || !response.data?.accessToken) {
            this.error.set(response.message || 'Invalid email or password.');
            return;
          }

          this.loadUserAndNavigate();
        },
        error: (httpError) => {
          this.error.set(
            httpError.error?.message || 'Invalid email or password. Please try again.',
          );
        },
      });
  }

  private loadUserAndNavigate(): void {
    this.authState
      .loadCurrentUser()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (!response.success || !response.data) {
            this.authState.logout();
            this.error.set('Unable to retrieve your account information.');
            return;
          }

          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

          this.router.navigateByUrl(
            returnUrl?.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/',
          );
        },
        error: () => {
          this.authState.logout();
          this.error.set(
            'Login succeeded, but we could not retrieve your account. Please try again.',
          );
        },
      });
  }
}
