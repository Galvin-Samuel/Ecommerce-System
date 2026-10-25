import { apiClient, setAuthToken } from './client';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../types/auth';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const data = await apiClient<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    if (data?.accessToken) {
      setAuthToken(data.accessToken);
    }
    return data;
  },

  register: async (payload: RegisterRequest): Promise<User> => {
    return apiClient<User>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  getMe: async (): Promise<User> => {
    return apiClient<User>('/api/v1/auth/me', {
      method: 'GET',
      requiresAuth: true,
    });
  },

  logout: () => {
    setAuthToken(null);
  },
};

