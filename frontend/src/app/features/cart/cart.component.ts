import { Component, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { CartService } from '../../core/services/cart.service';
import { CartItem } from '../../core/models/cart-item.model';
import { CustomerOrderApiService } from '../../core/services/customer-order-api.service';
import { AuthStateService } from '../../core/services/auth-state.service';
import { StoreHeaderComponent } from '../../shared/components/store-header/store-header.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, StoreHeaderComponent],
  templateUrl: './cart.component.html',
})
export class CartComponent {
  readonly cart = inject(CartService);

  private readonly orderApi = inject(CustomerOrderApiService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly checkingOut = signal(false);
  readonly checkoutError = signal('');

  formatPrice(price: number, currency: string): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency,
      maximumFractionDigits: 2,
    }).format(price);
  }

  increaseQuantity(item: CartItem): void {
    if (item.productType === 'PHYSICAL') {
      this.cart.updateQuantity(item.productType, item.productId, item.quantity + 1);
    }
  }

  decreaseQuantity(item: CartItem): void {
    if (item.productType === 'PHYSICAL' && item.quantity > 1) {
      this.cart.updateQuantity(item.productType, item.productId, item.quantity - 1);
    }
  }

  removeItem(item: CartItem): void {
    this.cart.removeItem(item.productType, item.productId);
    this.checkoutError.set('');
  }

  checkout(): void {
    if (this.checkingOut()) {
      return;
    }

    this.checkoutError.set('');

    if (!this.authState.isAuthenticated()) {
      this.router.navigate(['/login'], {
        queryParams: {
          returnUrl: '/cart',
        },
      });
      return;
    }

    const items = this.cart.items();

    if (items.length === 0) {
      this.checkoutError.set('Your cart is empty.');
      return;
    }

    const physicalItems = items.filter((item) => item.productType !== 'DIGITAL');

    if (physicalItems.length > 0) {
      this.checkoutError.set(
        'Physical books are not available for checkout yet. Please remove them and try again.',
      );
      return;
    }

    const ebookIds = items.map((item) => item.productId);

    if (
      ebookIds.length > 20 ||
      ebookIds.some((id) => !Number.isInteger(id) || id <= 0) ||
      new Set(ebookIds).size !== ebookIds.length ||
      items.some((item) => item.quantity !== 1)
    ) {
      this.checkoutError.set(
        'Your cart contains invalid items. Please review your cart and try again.',
      );
      return;
    }

    this.checkingOut.set(true);

    this.orderApi
      .createOrder(ebookIds)
      .pipe(
        finalize(() => this.checkingOut.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (!response.success || !response.data) {
            this.checkoutError.set(
              response.message || 'Unable to create your order. Please try again.',
            );
            return;
          }

          // Clear the cart only after the backend confirms order creation.
          this.cart.clear();

          this.router.navigate(['/account/orders']);
        },
        error: (httpError) => {
          this.checkoutError.set(
            httpError.error?.message ||
              'Unable to create your order. Please check your cart and try again.',
          );
        },
      });
  }
}
