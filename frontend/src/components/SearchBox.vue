<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSearch, highlightMatch } from '@/composables/useSearch'
import { getHotCompanies } from '@/api'
import type { Company } from '@/types'

const { inputRef, rootRef, onInput, onKeydown, selectCompany } = useSearch()
const store = useSearchStore()
const router = useRouter()
import { useSearchStore } from '@/stores'

const hotCompanies = ref<Company[]>([])

// 加载热门公司
getHotCompanies().then(res => hotCompanies.value = res.data.data)

function onFocus() {
  if (!store.query.trim()) {
    getHotCompanies().then(res => {
      store.setResults('', res.data.data)
    })
  }
}

function onDocumentPointerDown(event: MouseEvent) {
  const target = event.target as Node | null
  if (rootRef.value && target && !rootRef.value.contains(target)) {
    store.close()
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocumentPointerDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentPointerDown)
})

// 搜索栏也用于 Navbar 中的内嵌搜索
const props = withDefaults(defineProps<{
  compact?: boolean
  placeholder?: string
}>(), {
  compact: false,
  placeholder: '搜索公司名称或股票代码...',
})
</script>

<template>
  <div ref="rootRef" class="search-box" :class="{ compact }">
    <div class="search-input-wrapper">
      <span class="search-icon">🔍</span>
      <input
        ref="inputRef"
        type="text"
        :placeholder="placeholder"
        autocomplete="off"
        @input="e => onInput((e.target as HTMLInputElement).value)"
        @keydown="onKeydown"
        @focus="onFocus"
      />
      <span v-if="store.isLoading" class="search-spinner" />
    </div>

    <!-- 搜索建议下拉 -->
    <div v-if="store.isOpen" class="search-dropdown">
      <div v-if="store.results.length === 0" class="search-empty">
        🔍 未找到匹配公司
      </div>
      <div
        v-for="(item, idx) in store.results"
        :key="item.code"
        class="search-item"
        :class="{ active: idx === store.selectedIndex }"
        @click="selectCompany(item)"
        @mouseenter="store.selectedIndex = idx"
      >
        <span class="item-name" v-html="highlightMatch(item.name, store.query)" />
        <span class="item-code">{{ item.code }}</span>
        <span class="item-tag">{{ item.industry }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-box {
  position: relative;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
}
.search-box.compact {
  max-width: 320px;
  margin: 0;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 0 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-input-wrapper:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}
.search-icon { font-size: 18px; margin-right: 8px; flex-shrink: 0; }
.search-box input {
  flex: 1;
  border: none;
  padding: 12px 0;
  font-size: 15px;
  background: transparent;
  box-shadow: none;
  border-radius: 0;
}
.search-box input:focus { box-shadow: none; }
.search-spinner {
  width: 16px; height: 16px;
  border: 2px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.search-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0; right: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  max-height: 360px;
  overflow-y: auto;
  z-index: 200;
}

.search-empty {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.search-item:hover, .search-item.active {
  background: var(--hover);
}
.item-name {
  flex: 1;
  font-weight: 500;
  font-size: 14px;
}
.item-code {
  font-size: 13px;
  color: var(--text-muted);
  font-family: monospace;
}
.item-tag {
  font-size: 12px;
  padding: 2px 8px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 4px;
}

:deep(.search-highlight) {
  background: #fef08a;
  color: var(--text);
  border-radius: 2px;
  padding: 0 1px;
}
</style>
