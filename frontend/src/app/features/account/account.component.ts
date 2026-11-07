import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthStateService } from '../../core/services/auth-state.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss',
})
export class AccountComponent {
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly currentUser = this.authState.currentUser;

  logout(): void {
    this.authState.logout();
    this.router.navigate(['/']);
  }
}
