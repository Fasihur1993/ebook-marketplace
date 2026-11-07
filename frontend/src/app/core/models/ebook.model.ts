export interface Ebook {
  id: number;
  title: string;
  slug: string;
  description: string;
  price: number;
  currency: string;
  categoryId: number;
  categoryName: string;
  authorId: number;
  pageCount: number | null;
  language: string;
  status: string;
  pdfUploaded: boolean;
  publishedAt: string | null;
  createdAt: string;
}

export interface Ebook {
  id: number;
  title: string;
  slug: string;
  description: string;
  price: number;
  currency: string;
  categoryId: number;
  categoryName: string;
  authorId: number;
  pageCount: number | null;
  language: string;
  status: string;
  pdfUploaded: boolean;
  publishedAt: string | null;
  createdAt: string;
}

export interface EbookListResponse {
  ebooks: Ebook[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EbookPreviewImage {
  pageNumber: number;
  imageUrl: string;
}
