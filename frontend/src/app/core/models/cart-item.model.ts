export type CartProductType = 'DIGITAL' | 'PHYSICAL';

export interface CartItem {
  productId: number;
  productType: CartProductType;
  title: string;
  slug: string;
  unitPrice: number;
  currency: string;
  quantity: number;
}
