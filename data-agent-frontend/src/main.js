/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { createApp } from 'vue';
import App from '@/App.vue';
import router from '@/router';
import axios from 'axios';
import { ElMessage } from 'element-plus';
import authService, { AUTH_TOKEN_HEADER } from '@/services/auth';

// 引入全局样式
import '@/styles/global.css';
import 'element-plus/dist/index.css';
import ElementPlus from 'element-plus';

axios.interceptors.request.use(config => {
  const token = authService.getToken();
  if (token) {
    config.headers[AUTH_TOKEN_HEADER] = token;
  }
  return config;
});

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      authService.clearSession();
      if (router.currentRoute.value.path !== '/login') {
        ElMessage.error('登录已失效，请重新登录');
        router.replace({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath },
        });
      }
    }
    return Promise.reject(error);
  }
);

// 创建应用实例
const app = createApp(App);
app.use(router);
app.use(ElementPlus);
app.mount('#app');
