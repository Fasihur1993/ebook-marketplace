export interface CreateEbookRequest {
  title: string;
  description: string;
  price: number;
  categoryId: number;
  pageCount: number | null;
  language: string;
}