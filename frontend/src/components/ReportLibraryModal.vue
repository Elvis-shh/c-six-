<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { deleteReportLibraryItem, getReportLibrary } from '@/api'
import type { ReportLibraryItem } from '@/types'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const keyword = ref('')
const reports = ref<ReportLibraryItem[]>([])
const deletingId = ref<number | null>(null)

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return reports.value
  return reports.value.filter(item =>
    item.companyCode.includes(q) ||
    item.companyName.toLowerCase().includes(q) ||
    String(item.reportYear).includes(q)
  )
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await getReportLibrary()
    reports.value = res.data.data || []
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载财报库失败'
  } finally {
    loading.value = false
  }
}

function openReport(item: ReportLibraryItem) {
  emit('close')
  router.push(`/dashboard/${item.companyCode}`)
}

async function remove(item: ReportLibraryItem) {
  if (!item.deletable || deletingId.value) return
  if (!window.confirm(`确认删除 ${item.companyName} ${item.reportYear} 年用户上传财报？`)) return
  deletingId.value = item.id
  error.value = ''
  try {
    await deleteReportLibraryItem(item.id)
    reports.value = reports.value.filter(r => r.id !== item.id)
  } catch (e: any) {
    error.value = e?.response?.data?.message || '删除失败'
  } finally {
    deletingId.value = null
  }
}

watch(() => props.open, open => { if (open) load() }, { immediate: true })
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="library-mask" @click.self="emit('close')">
      <section class="library-panel" role="dialog" aria-modal="true" aria-label="财报库">
        <header class="library-header">
          <div>
            <h2>财报库</h2>
            <p>当前系统内可分析的财报，系统内置财报不可删除。</p>
          </div>
          <button class="library-close" @click="emit('close')">×</button>
        </header>

        <div class="library-toolbar">
          <input v-model="keyword" placeholder="搜索公司、代码或年份" />
          <button @click="load" :disabled="loading">刷新</button>
        </div>

        <p v-if="error" class="library-error">{{ error }}</p>
        <div v-if="loading" class="library-empty">正在加载财报库...</div>
        <div v-else-if="filtered.length === 0" class="library-empty">暂无财报</div>
        <div v-else class="library-list">
          <article v-for="item in filtered" :key="item.id" class="library-item">
            <button class="library-main" @click="openReport(item)">
              <strong>{{ item.companyName }}</strong>
              <span>{{ item.companyCode }} · {{ item.reportYear }} 年报</span>
            </button>
            <span class="library-source" :class="{ upload: item.source === 'upload' }">{{ item.sourceLabel }}</span>
            <button
              class="library-delete"
              :disabled="!item.deletable || deletingId === item.id"
              :title="item.deletable ? '删除用户上传财报' : '系统内置财报不能删除'"
              @click="remove(item)"
            >
              {{ deletingId === item.id ? '删除中' : '删除' }}
            </button>
          </article>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.library-mask {
  position: fixed;
  inset: 0;
  z-index: 500;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 86px 18px 24px;
  background: rgba(15, 23, 42, 0.35);
}
.library-panel {
  width: min(760px, 100%);
  max-height: calc(100vh - 120px);
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: var(--surface);
  box-shadow: var(--shadow-lg);
}
.library-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid var(--border-light);
}
.library-header h2 { margin: 0; font-size: 20px; }
.library-header p { margin-top: 4px; color: var(--text-secondary); font-size: 13px; }
.library-close {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: var(--surface-alt);
  color: var(--text-secondary);
  font-size: 22px;
}
.library-toolbar {
  display: flex;
  gap: 10px;
  padding: 14px 22px;
}
.library-toolbar input { flex: 1; }
.library-toolbar button {
  padding: 8px 16px;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
}
.library-error { padding: 0 22px 12px; color: var(--risk-red); font-size: 13px; }
.library-empty { padding: 32px; text-align: center; color: var(--text-muted); }
.library-list {
  max-height: 56vh;
  overflow: auto;
  padding: 4px 14px 18px;
}
.library-item {
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid var(--border-light);
}
.library-main {
  display: grid;
  gap: 2px;
  background: none;
  text-align: left;
  color: var(--text);
}
.library-main strong { font-size: 15px; }
.library-main span { color: var(--text-muted); font-size: 12px; }
.library-source {
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--surface-alt);
  color: var(--text-secondary);
  font-size: 12px;
}
.library-source.upload {
  background: var(--primary-light);
  color: var(--primary-dark);
}
.library-delete {
  padding: 6px 10px;
  border-radius: 8px;
  background: var(--risk-red-bg);
  color: var(--risk-red);
}
.library-delete:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
@media (max-width: 640px) {
  .library-item { grid-template-columns: 1fr; align-items: stretch; }
  .library-delete { justify-self: start; }
}
</style>
