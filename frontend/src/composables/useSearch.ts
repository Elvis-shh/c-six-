// composables/useSearch.ts — 搜索逻辑（防抖 + 键盘导航 + 请求取消）
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSearchStore } from '@/stores'
import { searchCompanies } from '@/api'
import type { Company } from '@/types'

export function useSearch() {
  const store = useSearchStore()
  const router = useRouter()

  let timer: ReturnType<typeof setTimeout> | null = null
  const inputRef = ref<HTMLInputElement | null>(null)
  const rootRef = ref<HTMLElement | null>(null)

  function onInput(q: string) {
    if (timer) clearTimeout(timer)
    if (!q.trim()) {
      store.close()
      return
    }
    store.isLoading = true
    timer = setTimeout(async () => {
      try {
        const res = await searchCompanies(q, 8)
        store.setResults(q, res.data.data)
      } catch {
        store.close()
      } finally {
        store.isLoading = false
      }
    }, 300)
  }

  function onKeydown(e: KeyboardEvent) {
    if (!store.isOpen) return
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault()
        store.moveDown()
        break
      case 'ArrowUp':
        e.preventDefault()
        store.moveUp()
        break
      case 'Enter':
        e.preventDefault()
        if (store.selectedIndex >= 0) {
          selectCompany(store.results[store.selectedIndex])
        }
        break
      case 'Escape':
        store.close()
        inputRef.value?.blur()
        break
    }
  }

  function selectCompany(company: Company) {
    store.close()
    router.push(`/dashboard/${company.code}`)
  }

  function focusInput() {
    inputRef.value?.focus()
  }

  return { inputRef, rootRef, onInput, onKeydown, selectCompany, focusInput }
}

// 高亮匹配关键词
export function highlightMatch(text: string, keyword: string): string {
  if (!keyword.trim()) return text
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}
