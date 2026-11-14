import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthStateService } from '../../../core/services/auth-state.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-store-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './store-header.component.html',
  styleUrl: './store-header.component.scss',
})
export class StoreHeaderComponent {
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);

  readonly cart = inject(CartService);
  readonly isAuthenticated = this.authState.isAuthenticated;
  readonly isAdmin = this.authState.isAdmin;
  readonly isMobileMenuOpen = signal(false);

  toggleMobileMenu(): void {
    this.isMobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen.set(false);
  }

  logout(): void {
    this.authState.logout();
    this.closeMobileMenu();
    this.router.navigate(['/']);
  }
}
