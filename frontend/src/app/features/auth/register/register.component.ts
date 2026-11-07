
import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authState = inject(AuthStateService);

  readonly loading = signal(false);
  readonly error = signal('');
  readonly registrationSuccess = signal(false);

  readonly registerForm = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.registerForm.invalid || this.loading()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.authState
      .register(this.registerForm.getRawValue())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          if (!response.success || !response.data) {
            this.error.set(
              response.errors?.join(' ') ||
              response.message ||
              'Unable to create your account.',
            );
            return;
          }

          this.registrationSuccess.set(true);
          this.registerForm.reset();
        },
        error: (httpError) => {
          this.error.set(
            httpError.error?.message ||
            httpError.error?.errors?.join(' ') ||
            'Unable to create your account. Please try again.',
          );
        },
      });
  }
}
