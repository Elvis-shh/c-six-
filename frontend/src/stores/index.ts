// stores/index.ts — Pinia 全局 Stores
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ChatMessage, Company } from '@/types'

// ============================================================
// 搜索状态
// ============================================================
export const useSearchStore = defineStore('search', () => {
  const query = ref('')
  const results = ref<Company[]>([])
  const isLoading = ref(false)
  const selectedIndex = ref(-1)
  const isOpen = ref(false)

  function setResults(q: string, list: Company[]) {
    query.value = q
    results.value = list
    isOpen.value = true
    selectedIndex.value = -1
  }

  function close() {
    isOpen.value = false
    selectedIndex.value = -1
  }

  function moveUp() {
    if (results.value.length === 0) return
    selectedIndex.value = selectedIndex.value <= 0
      ? results.value.length - 1 : selectedIndex.value - 1
  }

  function moveDown() {
    if (results.value.length === 0) return
    selectedIndex.value = selectedIndex.value >= results.value.length - 1
      ? 0 : selectedIndex.value + 1
  }

  return { query, results, isLoading, selectedIndex, isOpen, setResults, close, moveUp, moveDown }
})

// ============================================================
// Dashboard 状态
// ============================================================
export const useDashboardStore = defineStore('dashboard', () => {
  const companyCode = ref('')
  const kpiData = ref<any>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  function setCompany(code: string) {
    companyCode.value = code
  }

  function setKpiData(data: any) {
    kpiData.value = data
    error.value = null
  }

  function setError(msg: string) {
    error.value = msg
    kpiData.value = null
  }

  return { companyCode, kpiData, loading, error, setCompany, setKpiData, setError }
})

// ============================================================
// Phase 3: AI 聊天状态
// ============================================================
export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const isOpen = ref(false)
  const isLoading = ref(false)
  const progress = ref(0)
  const sessionId = ref(globalThis.crypto?.randomUUID?.() || String(Date.now()))

  function addMessage(role: ChatMessage['role'], content: string) {
    messages.value.push({ role, content, timestamp: Date.now() })
  }

  function ensureAssistantMessage() {
    const last = messages.value[messages.value.length - 1]
    if (last?.role !== 'assistant') {
      messages.value.push({ role: 'assistant', content: '', timestamp: Date.now() })
    }
  }

  function updateLastAssistant(content: string, refs?: ChatMessage['refs'], followUps?: string[]) {
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant') {
      last.content = content
      if (refs) last.refs = refs
      if (followUps) last.followUps = followUps
    } else {
      messages.value.push({ role: 'assistant', content, refs, followUps, timestamp: Date.now() })
    }
  }

  function toggle() { isOpen.value = !isOpen.value }
  function close() { isOpen.value = false }

  return { messages, isOpen, isLoading, progress, sessionId, addMessage, ensureAssistantMessage, updateLastAssistant, toggle, close }
})
