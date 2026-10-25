import { ApiResponse, ApiErrorResponse } from '../types/api';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export class ApiError extends Error {
  status: number;
  errorResponse?: ApiErrorResponse;

  constructor(status: number, message: string, errorResponse?: ApiErrorResponse) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorResponse = errorResponse;
  }
}

let onUnauthorizedCallback: (() => void) | null = null;

export const setOnUnauthorizedCallback = (callback: () => void) => {
  onUnauthorizedCallback = callback;
};

export const getAuthToken = (): string | null => {
  try {
    return localStorage.getItem('nova_access_token');
  } catch {
    return null;
  }
};

export const setAuthToken = (token: string | null) => {
  try {
    if (token) {
      localStorage.setItem('nova_access_token', token);
    } else {
      localStorage.removeItem('nova_access_token');
    }
  } catch {
    // Ignore storage errors in restricted contexts
  }
};

interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
}

export async function apiClient<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { requiresAuth = false, headers = {}, ...restOptions } = options;

  const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(headers as Record<string, string>),
  };

  const token = getAuthToken();
  if (token) {
    requestHeaders['Authorization'] = `Bearer ${token}`;
  } else if (requiresAuth) {
    throw new ApiError(401, 'Authentication required');
  }

  try {
    const response = await fetch(url, {
      ...restOptions,
      headers: requestHeaders,
    });

    // 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    let responseData: any = null;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      responseData = await response.json();
    } else {
      const text = await response.text();
      responseData = { message: text };
    }

    if (!response.ok) {
      if (response.status === 401 && onUnauthorizedCallback) {
        setAuthToken(null);
        onUnauthorizedCallback();
      }

      const errorMessage =
        responseData?.message ||
        responseData?.error ||
        `Request failed with status ${response.status}`;

      throw new ApiError(response.status, errorMessage, responseData as ApiErrorResponse);
    }

    // Unpack standard ApiResponse envelope if present
    if (responseData && typeof responseData === 'object' && 'success' in responseData && 'data' in responseData) {
      return (responseData as ApiResponse<T>).data;
    }

    return responseData as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, error instanceof Error ? error.message : 'Network connection failure');
  }
}

