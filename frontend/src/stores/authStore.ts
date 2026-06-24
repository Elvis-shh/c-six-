// stores/authStore.ts — Pinia 认证状态
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const USER_STORAGE_KEY = 'smartreport_user'

export interface UserInfo {
  userId: number
  email: string
  nickname: string
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const user = ref<UserInfo | null>(loadStoredUser())

  const isLoggedIn = computed(() => !!accessToken.value)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
  }

  function setUser(u: UserInfo) {
    user.value = u
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(u))
  }

  function clearUser() {
    user.value = null
    localStorage.removeItem(USER_STORAGE_KEY)
  }

  function logout() {
    accessToken.value = null
    refreshToken.value = null
    clearUser()
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return { accessToken, refreshToken, user, isLoggedIn, setTokens, setUser, clearUser, logout }
})

function loadStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    localStorage.removeItem(USER_STORAGE_KEY)
    return null
  }
}
