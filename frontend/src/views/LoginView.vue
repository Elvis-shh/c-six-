<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { registerUser, loginUser } from '@/api'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const isRegister = ref(false)
const email = ref('')
const password = ref('')
const nickname = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '请填写邮箱和密码'
    return
  }
  loading.value = true
  try {
    const res = isRegister.value
      ? await registerUser({ email: email.value, password: password.value, nickname: nickname.value || undefined })
      : await loginUser({ email: email.value, password: password.value })
    const d = res.data.data
    auth.setTokens(d.accessToken, d.refreshToken)
    auth.setUser({ userId: d.userId, email: d.email, nickname: d.nickname })
    const redirect = (route.query.redirect as string) || '/search'
    router.push(redirect)
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand" @click="router.push('/search')">📊 SmartReport</div>
      <h2>{{ isRegister ? '创建账号' : '欢迎回来' }}</h2>
      <p class="login-sub">{{ isRegister ? '注册后同步分析历史' : '登录以同步分析历史' }}</p>

      <div v-if="error" class="login-error">{{ error }}</div>

      <form @submit.prevent="submit">
        <label>邮箱</label>
        <input v-model="email" type="email" placeholder="your@email.com" autocomplete="email" />

        <label>密码</label>
        <input v-model="password" type="password" placeholder="至少6位" autocomplete="current-password" />

        <label v-if="isRegister">昵称（选填）</label>
        <input v-if="isRegister" v-model="nickname" placeholder="怎么称呼你" />

        <button type="submit" :disabled="loading" class="btn-submit">
          {{ loading ? '处理中...' : isRegister ? '注册' : '登录' }}
        </button>
      </form>

      <p class="login-switch">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <a href="#" @click.prevent="isRegister = !isRegister; error = ''">
          {{ isRegister ? '去登录' : '去注册' }}
        </a>
      </p>

      <p class="login-back">
        <a href="#" @click.prevent="router.push('/search')">← 返回首页</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8ecf8 100%);
  padding: 20px;
}
.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  width: 400px;
  max-width: 100%;
  box-shadow: 0 8px 40px rgba(0,0,0,.08);
}
.login-brand {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #3b82f6;
  margin-bottom: 24px;
  cursor: pointer;
}
.login-card h2 {
  margin: 0 0 4px;
  font-size: 24px;
  color: #1f2937;
}
.login-sub {
  margin: 0 0 20px;
  font-size: 14px;
  color: #9ca3af;
}
.login-error {
  color: #ef4444;
  font-size: 13px;
  margin-bottom: 14px;
  padding: 10px;
  background: #fef2f2;
  border-radius: 8px;
}
label {
  display: block;
  font-size: 13px;
  color: #6b7280;
  margin: 12px 0 4px;
  font-weight: 500;
}
input {
  width: 100%;
  padding: 11px 14px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  font-size: 15px;
  box-sizing: border-box;
  transition: border-color .15s;
}
input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,.1); }
.btn-submit {
  width: 100%;
  margin-top: 22px;
  padding: 12px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}
.btn-submit:hover { background: #2563eb; }
.btn-submit:disabled { opacity: .5; cursor: not-allowed; }
.login-switch {
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  margin: 18px 0 0;
}
.login-switch a { color: #3b82f6; font-weight: 500; }
.login-back {
  text-align: center;
  margin: 14px 0 0;
}
.login-back a { color: #9ca3af; font-size: 13px; }
</style>
