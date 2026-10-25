import { apiClient } from './client';
import { Cart } from '../types/cart';

export const cartApi = {
  getCart: async (): Promise<Cart> => {
    return apiClient<Cart>('/api/v1/cart', {
      method: 'GET',
      requiresAuth: true,
    });
  },

  addItem: async (productId: string, quantity: number = 1): Promise<Cart> => {
    return apiClient<Cart>('/api/v1/cart/items', {
      method: 'POST',
      requiresAuth: true,
      body: JSON.stringify({ productId, quantity }),
    });
  },

  updateQuantity: async (productId: string, quantity: number): Promise<Cart> => {
    return apiClient<Cart>(`/api/v1/cart/items/${productId}`, {
      method: 'PUT',
      requiresAuth: true,
      body: JSON.stringify({ quantity }),
    });
  },

  removeItem: async (productId: string): Promise<void> => {
    return apiClient<void>(`/api/v1/cart/items/${productId}`, {
      method: 'DELETE',
      requiresAuth: true,
    });
  },

  clearCart: async (): Promise<void> => {
    return apiClient<void>('/api/v1/cart', {
      method: 'DELETE',
      requiresAuth: true,
    });
  },
};

