<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="brand"><i class="bi bi-robot"></i><span>Data Agent</span></div>
      <h1 id="login-title">登录</h1>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="submit">
        <el-form-item prop="loginName">
          <el-input v-model="form.loginName" autocomplete="username" placeholder="LDAP 用户名" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" placeholder="密码" size="large" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" native-type="submit" :loading="loading" class="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import authService from '@/services/auth';

const router = useRouter();
const route = useRoute();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ loginName: '', password: '' });
const rules: FormRules = {
  loginName: [{ required: true, message: '请输入 LDAP 用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await authService.login(form.loginName, form.password);
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/agents';
    await router.replace(redirect);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; background: #f4f7fb; padding: 24px; }
.login-panel { width: min(100%, 380px); background: #fff; border: 1px solid #dce3ed; border-radius: 8px; padding: 32px; }
.brand { color: #1778c9; font-size: 20px; font-weight: 600; display: flex; gap: 10px; align-items: center; }
h1 { font-size: 24px; margin: 28px 0 20px; color: #1f2937; }
.submit { width: 100%; }
</style>
