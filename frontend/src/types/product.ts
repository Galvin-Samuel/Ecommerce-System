export interface Product {
  id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  stockQuantity: number;
  imageUrl?: string;
  sellerId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSuggestion {
  id: string;
  name: string;
  category: string;
  price: number;
}

