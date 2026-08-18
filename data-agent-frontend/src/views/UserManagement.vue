<template>
  <BaseLayout>
    <main class="users-page">
      <div class="page-heading"><h1>用户管理</h1><el-button :icon="Refresh" @click="load" :loading="loading">刷新</el-button></div>
      <el-table :data="users" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="100" />
        <el-table-column prop="loginName" label="登录名" min-width="240" />
        <el-table-column label="权限" width="140"><template #default="{ row }"><el-tag :type="row.admin ? 'danger' : 'info'">{{ row.admin ? '管理员' : '普通用户' }}</el-tag></template></el-table-column>
      </el-table>
    </main>
  </BaseLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import BaseLayout from '@/layouts/BaseLayout.vue';
import type { AuthUser } from '@/services/auth';
import { listUsers } from '@/services/user';

const users = ref<AuthUser[]>([]);
const loading = ref(false);
const load = async () => {
  loading.value = true;
  try { users.value = await listUsers(); } catch { ElMessage.error('获取用户列表失败'); } finally { loading.value = false; }
};
onMounted(load);
</script>

<style scoped>
.users-page { max-width: 1120px; margin: 0 auto; padding: 28px 24px; }
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h1 { margin: 0; font-size: 24px; color: #1f2937; }
</style>
