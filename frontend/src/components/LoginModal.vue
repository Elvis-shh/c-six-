<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { registerUser, loginUser } from '@/api'
import type { AuthResponse } from '@/types'

const emit = defineEmits<{ (e: 'close'): void }>()
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
    let res: any
    if (isRegister.value) {
      res = await registerUser({ email: email.value, password: password.value, nickname: nickname.value || undefined })
    } else {
      res = await loginUser({ email: email.value, password: password.value })
    }
    const d = res.data.data
    auth.setTokens(d.accessToken, d.refreshToken)
    auth.setUser({ userId: d.userId, email: d.email, nickname: d.nickname })
    emit('close')
  } catch (e: any) {
    error.value = e?.response?.data?.message || e?.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal-card">
      <h2>{{ isRegister ? '注册' : '登录' }}</h2>
      <div v-if="error" class="modal-error">{{ error }}</div>
      <form @submit.prevent="submit">
        <label>邮箱</label>
        <input v-model="email" type="email" placeholder="your@email.com" />
        <label>密码</label>
        <input v-model="password" type="password" placeholder="至少6位" />
        <label v-if="isRegister">昵称（选填）</label>
        <input v-if="isRegister" v-model="nickname" placeholder="怎么称呼你" />
        <button type="submit" :disabled="loading" class="btn-primary">
          {{ loading ? '处理中...' : isRegister ? '注册' : '登录' }}
        </button>
      </form>
      <p class="modal-switch">
        {{ isRegister ? '已有账号？' : '没有账号？' }}
        <a href="#" @click.prevent="isRegister = !isRegister; error = ''">
          {{ isRegister ? '去登录' : '去注册' }}
        </a>
      </p>
      <button class="modal-close" @click="emit('close')">✕</button>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,.4);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.modal-card {
  background: #fff; border-radius: 14px; padding: 32px 28px; width: 380px;
  position: relative; box-shadow: 0 12px 40px rgba(0,0,0,.15);
}
.modal-card h2 { margin: 0 0 16px; font-size: 20px; color: #1f2937; }
.modal-error { color: #ef4444; font-size: 13px; margin-bottom: 12px; padding: 8px; background: #fef2f2; border-radius: 6px; }
label { display: block; font-size: 13px; color: #6b7280; margin: 10px 0 4px; }
input { width: 100%; padding: 10px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; box-sizing: border-box; }
input:focus { outline: none; border-color: #3b82f6; }
.btn-primary { width: 100%; margin-top: 18px; padding: 11px; background: #3b82f6; color: #fff; border: none; border-radius: 8px; font-size: 15px; cursor: pointer; }
.btn-primary:disabled { opacity: .6; cursor: not-allowed; }
.modal-switch { text-align: center; font-size: 13px; color: #9ca3af; margin: 14px 0 0; }
.modal-switch a { color: #3b82f6; }
.modal-close { position: absolute; top: 12px; right: 14px; background: none; border: none; font-size: 18px; cursor: pointer; color: #9ca3af; }
</style>
