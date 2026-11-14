import { computed, inject, Injectable, PLATFORM_ID, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

import { Ebook } from '../models/ebook.model';
import { CartItem } from '../models/cart-item.model';

@Injectable({
  providedIn: 'root',
})
export class CartService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'ebook-marketplace-cart-v1';

  readonly items = signal<CartItem[]>(this.loadCart());

  readonly itemCount = computed(() =>
    this.items().reduce((total, item) => total + item.quantity, 0),
  );

  readonly uniqueItemCount = computed(() => this.items().length);

  readonly subtotal = computed(() =>
    this.items().reduce((total, item) => total + item.unitPrice * item.quantity, 0),
  );

  readonly currency = computed(() => this.items()[0]?.currency ?? 'INR');

  addEbook(ebook: Ebook): boolean {
    if (
      this.items().some((item) => item.productType === 'DIGITAL' && item.productId === ebook.id)
    ) {
      return false;
    }

    const item: CartItem = {
      productId: ebook.id,
      productType: 'DIGITAL',
      title: ebook.title,
      slug: ebook.slug,
      unitPrice: ebook.price,
      currency: ebook.currency,
      quantity: 1,
    };

    this.items.update((current) => [...current, item]);
    this.persistCart();

    return true;
  }

  addPhysicalBook(book: Omit<CartItem, 'productType' | 'quantity'>): void {
    const existing = this.items().find(
      (item) => item.productType === 'PHYSICAL' && item.productId === book.productId,
    );

    if (existing) {
      this.updateQuantity('PHYSICAL', book.productId, existing.quantity + 1);
      return;
    }

    const item: CartItem = {
      ...book,
      productType: 'PHYSICAL',
      quantity: 1,
    };

    this.items.update((current) => [...current, item]);
    this.persistCart();
  }

  updateQuantity(productType: CartItem['productType'], productId: number, quantity: number): void {
    if (!Number.isInteger(quantity) || quantity < 1) {
      return;
    }

    this.items.update((current) =>
      current.map((item) => {
        if (item.productType !== productType || item.productId !== productId) {
          return item;
        }

        return {
          ...item,
          quantity: item.productType === 'DIGITAL' ? 1 : quantity,
        };
      }),
    );

    this.persistCart();
  }

  removeItem(productType: CartItem['productType'], productId: number): void {
    this.items.update((current) =>
      current.filter((item) => item.productType !== productType || item.productId !== productId),
    );

    this.persistCart();
  }

  clear(): void {
    this.items.set([]);
    this.persistCart();
  }

  isInCart(productId: number, productType: CartItem['productType'] = 'DIGITAL'): boolean {
    return this.items().some(
      (item) => item.productId === productId && item.productType === productType,
    );
  }

  private loadCart(): CartItem[] {
    if (!isPlatformBrowser(this.platformId)) {
      return [];
    }

    try {
      const storedCart = localStorage.getItem(this.storageKey);

      if (!storedCart) {
        return [];
      }

      const parsed: unknown = JSON.parse(storedCart);

      if (!Array.isArray(parsed)) {
        return [];
      }

      return parsed.filter(
        (item): item is CartItem =>
          item !== null &&
          typeof item === 'object' &&
          Number.isInteger(item.productId) &&
          item.productId > 0 &&
          (item.productType === 'DIGITAL' || item.productType === 'PHYSICAL') &&
          typeof item.title === 'string' &&
          typeof item.slug === 'string' &&
          typeof item.unitPrice === 'number' &&
          Number.isFinite(item.unitPrice) &&
          item.unitPrice >= 0 &&
          typeof item.currency === 'string' &&
          Number.isInteger(item.quantity) &&
          item.quantity >= 1 &&
          (item.productType !== 'DIGITAL' || item.quantity === 1),
      );
    } catch {
      return [];
    }
  }

  private persistCart(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      localStorage.setItem(this.storageKey, JSON.stringify(this.items()));
    } catch (error) {
      console.error('Unable to persist shopping cart:', error);
    }
  }
}
