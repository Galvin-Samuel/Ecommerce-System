import { apiClient } from './client';

export interface ShortUrlResponse {
  shortCode: string;
  originalUrl: string;
  shortUrl: string;
  clickCount: number;
  createdAt: string;
}

export const shortUrlApi = {
  createShortUrl: async (url: string, customAlias?: string): Promise<ShortUrlResponse> => {
    return apiClient<ShortUrlResponse>('/api/v1/short-urls', {
      method: 'POST',
      body: JSON.stringify({
        originalUrl: url,
        customAlias: customAlias || undefined,
      }),
    });
  },
};
