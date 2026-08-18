import axios from 'axios';
import type { ApiResponse } from './common';
import type { AuthUser } from './auth';

export async function listUsers(): Promise<AuthUser[]> {
  const response = await axios.get<ApiResponse<AuthUser[]>>('/api/users');
  return response.data.data || [];
}
