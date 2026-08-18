import axios from 'axios';
import type { ApiResponse } from './common';

export const AUTH_TOKEN_HEADER = 'data_agent_access_token';
const TOKEN_STORAGE_KEY = 'data_agent_token';
const USER_STORAGE_KEY = 'data_agent_user';

export interface AuthUser {
  id: number;
  loginName: string;
  admin: boolean;
}

interface LoginResponse {
  token: string;
  user: AuthUser;
}

class AuthService {
  private enabled: boolean | null = null;

  async isEnabled(): Promise<boolean> {
    if (this.enabled !== null) return this.enabled;
    const response = await axios.get<ApiResponse<boolean>>('/api/auth/status');
    this.enabled = Boolean(response.data.data);
    return this.enabled;
  }

  getToken(): string {
    return localStorage.getItem(TOKEN_STORAGE_KEY) || '';
  }

  getStoredUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      this.clearSession();
      return null;
    }
  }

  async login(loginName: string, password: string): Promise<AuthUser> {
    const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/login', { loginName, password });
    const data = response.data.data;
    if (!response.data.success || !data?.token || !data.user) {
      throw new Error(response.data.message || '登录失败');
    }
    localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(data.user));
    return data.user;
  }

  async fetchCurrentUser(): Promise<AuthUser> {
    const response = await axios.get<ApiResponse<AuthUser>>('/api/auth/me');
    const user = response.data.data;
    if (!response.data.success || !user) {
      throw new Error(response.data.message || '登录已失效');
    }
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    return user;
  }

  async logout(): Promise<void> {
    try {
      await axios.post('/api/auth/logout');
    } finally {
      this.clearSession();
    }
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  }
}

export default new AuthService();
