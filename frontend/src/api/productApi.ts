import { apiClient } from './client';
import { Product, ProductSuggestion } from '../types/product';
import { PagedResponse } from '../types/api';

export interface ProductQueryParams {
  category?: string;
  q?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export const productApi = {
  getProducts: async (params: ProductQueryParams = {}): Promise<PagedResponse<Product>> => {
    const query = new URLSearchParams();
    if (params.category) query.set('category', params.category);
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.sort) query.set('sort', params.sort);

    const queryString = query.toString();
    const endpoint = `/api/v1/products${queryString ? `?${queryString}` : ''}`;
    return apiClient<PagedResponse<Product>>(endpoint, { method: 'GET' });
  },

  searchProducts: async (params: ProductQueryParams = {}): Promise<PagedResponse<Product>> => {
    const query = new URLSearchParams();
    if (params.q) query.set('q', params.q);
    if (params.category) query.set('category', params.category);
    if (params.minPrice !== undefined) query.set('minPrice', params.minPrice.toString());
    if (params.maxPrice !== undefined) query.set('maxPrice', params.maxPrice.toString());
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.sort) query.set('sort', params.sort);

    const queryString = query.toString();
    const endpoint = `/api/v1/products/search${queryString ? `?${queryString}` : ''}`;
    return apiClient<PagedResponse<Product>>(endpoint, { method: 'GET' });
  },

  getProductById: async (id: string): Promise<Product> => {
    return apiClient<Product>(`/api/v1/products/${id}`, { method: 'GET' });
  },

  getSuggestions: async (query: string, limit: number = 8): Promise<ProductSuggestion[]> => {
    const searchParams = new URLSearchParams({ q: query, limit: limit.toString() });
    return apiClient<ProductSuggestion[]>(`/api/v1/products/suggestions?${searchParams.toString()}`, {
      method: 'GET',
    });
  },
};

