export interface CartItem {
  id: string;
  productId: string;
  productName: string;
  productImageUrl?: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  addedAt: string;
}

export interface Cart {
  items: CartItem[];
  totalPrice: number;
  totalItems: number;
}

