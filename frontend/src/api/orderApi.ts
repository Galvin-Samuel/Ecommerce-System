import { apiClient } from './client';
import { Order } from '../types/order';

export const orderApi = {
  createOrder: async (): Promise<Order> => {
    return apiClient<Order>('/api/v1/orders', {
      method: 'POST',
      requiresAuth: true,
      body: JSON.stringify({}),
    });
  },

  getOrders: async (): Promise<Order[]> => {
    return apiClient<Order[]>('/api/v1/orders', {
      method: 'GET',
      requiresAuth: true,
    });
  },

  getOrderById: async (orderId: string): Promise<Order> => {
    return apiClient<Order>(`/api/v1/orders/${orderId}`, {
      method: 'GET',
      requiresAuth: true,
    });
  },
};

