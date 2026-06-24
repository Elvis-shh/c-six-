// composables/useHistory.ts — 分析历史管理（localStorage + 云端同步）
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { getHistory, addHistoryItem, deleteHistoryItem, clearHistory, syncHistory } from '@/api'

const STORAGE_KEY = 'smartreport_history'
const MAX_ITEMS = 20
const items = ref<HistoryRecord[]>([])

export interface HistoryRecord {
  code: string
  name: string
  timestamp: number
  id?: number
  reportYear?: number
  source?: string
  sourceLabel?: string
}

export function useHistory() {
  const auth = useAuthStore()

  function loadLocal(): HistoryRecord[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch { return [] }
  }

  function saveLocal(list: HistoryRecord[]) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(list.slice(0, MAX_ITEMS)))
  }

  async function loadRemote() {
    try {
      const res = await getHistory()
      if (res.data.code === 0 && res.data.data) {
        items.value = res.data.data.map((h: any) => ({
          code: h.code,
          name: h.name,
          timestamp: h.timestamp,
          id: h.id,
          reportYear: h.reportYear,
          source: h.source,
          sourceLabel: h.sourceLabel,
        }))
      }
    } catch { /* 未登录或网络错误，使用本地数据 */ }
  }

  async function load() {
    if (auth.isLoggedIn) {
      // 登录：先上传本地数据，再拉取云端
      const localItems = loadLocal()
      if (localItems.length > 0) {
        try {
          const res = await syncHistory(localItems.map(i => ({ code: i.code, name: i.name, timestamp: i.timestamp })))
          if (res.data.code === 0 && res.data.data) {
            items.value = res.data.data.map((h: any) => ({ code: h.code, name: h.name, timestamp: h.timestamp, id: h.id, reportYear: h.reportYear, source: h.source, sourceLabel: h.sourceLabel }))
            localStorage.removeItem(STORAGE_KEY) // 清除本地
            return
          }
        } catch { /* fallback */ }
      }
      await loadRemote()
    } else {
      // 未登录：从本地加载
      items.value = loadLocal()
    }
  }

  async function add(company: { code: string; name: string; reportYear?: number; sourceLabel?: string }) {
    const record: HistoryRecord = { code: company.code, name: company.name, reportYear: company.reportYear, sourceLabel: company.sourceLabel, timestamp: Date.now() }
    items.value = items.value.filter(i => i.code !== company.code)
    items.value.unshift(record)
    if (items.value.length > MAX_ITEMS) {
      items.value = items.value.slice(0, MAX_ITEMS)
    }

    if (auth.isLoggedIn) {
      try { await addHistoryItem(company.code, company.name) } catch { /* ignore */ }
    } else {
      saveLocal(items.value)
    }
  }

  async function remove(code: string) {
    const item = items.value.find(i => i.code === code)
    items.value = items.value.filter(i => i.code !== code)
    if (auth.isLoggedIn && item?.id) {
      try { await deleteHistoryItem(item.id) } catch { /* ignore */ }
    }
    if (!auth.isLoggedIn) saveLocal(items.value)
  }

  async function clear() {
    items.value = []
    if (auth.isLoggedIn) {
      try { await clearHistory() } catch { /* ignore */ }
    }
    localStorage.removeItem(STORAGE_KEY)
  }

  // 监听登录状态变化
  watch(() => auth.isLoggedIn, (loggedIn) => {
    if (loggedIn) {
      load()
    } else {
      items.value = loadLocal()
    }
  })

  onMounted(load)

  return { items, add, remove, clear }
}

// 相对时间格式化
export function relativeTime(ts: number): string {
  const diff = Date.now() - ts
  const sec = Math.floor(diff / 1000)
  if (sec < 60) return '刚刚'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min} 分钟前`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr} 小时前`
  const day = Math.floor(hr / 24)
  if (day < 7) return `${day} 天前`
  return new Date(ts).toLocaleDateString('zh-CN')
}
