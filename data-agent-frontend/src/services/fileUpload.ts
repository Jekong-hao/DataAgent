/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 业务API服务
 * 封装所有业务相关的API调用
 */

interface UploadResponse {
  success: boolean;
  message?: string;
  url?: string;
}

const AUTH_TOKEN_HEADER = 'data_agent_access_token';
const AUTH_TOKEN_STORAGE_KEY = 'data_agent_token';

// 文件上传API
export const fileUploadApi = {
  // 上传头像
  uploadAvatar(file: File): Promise<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    const url = '/api/upload/avatar';
    const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) || '';
    return fetch(url, {
      method: 'POST',
      headers: token ? { [AUTH_TOKEN_HEADER]: token } : undefined,
      body: formData,
    }).then(async response => {
      if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(`Upload failed: ${response.status} ${text}`);
      }
      const ct = response.headers.get('content-type') || '';
      if (ct.includes('application/json')) {
        return await response.json();
      }
      const text = await response.text();
      return { success: true, message: 'ok', url: text };
    });
  },
};
