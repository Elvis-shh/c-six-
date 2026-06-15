// composables/useHistory.ts — 分析历史管理（localStorage 持久化）
import { ref, onMounted } from 'vue'
import type { AnalysisHistoryItem } from '@/types'

const STORAGE_KEY = 'smartreport_history'
const MAX_ITEMS = 20

export function useHistory() {
  const items = ref<AnalysisHistoryItem[]>([])

  function load() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      items.value = raw ? JSON.parse(raw) : []
    } catch {
      items.value = []
    }
  }

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items.value))
  }

  function add(company: { code: string; name: string }) {
    items.value = items.value.filter(i => i.code !== company.code)
    items.value.unshift({ ...company, timestamp: Date.now() })
    if (items.value.length > MAX_ITEMS) {
      items.value = items.value.slice(0, MAX_ITEMS)
    }
    save()
  }

  function remove(code: string) {
    items.value = items.value.filter(i => i.code !== code)
    save()
  }

  function clear() {
    items.value = []
    save()
  }

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
