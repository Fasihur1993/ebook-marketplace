export interface PurchasedEbook {
  ebookId: number;
  title: string;
  slug: string;
  description: string;
  price: number;
  currency: string;
  language: string;
  pageCount: number;
  purchasedAt: string;
}
