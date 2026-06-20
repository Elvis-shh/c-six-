<script setup lang="ts">
import { ref, watch } from 'vue'
import { getHighlights } from '@/api'
import type { HighlightItem } from '@/types'

const props = defineProps<{ companyCode: string }>()

const loading = ref(false)
const items = ref<HighlightItem[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getHighlights(props.companyCode)
    items.value = res.data.data ?? []
  } finally {
    loading.value = false
  }
}

watch(() => props.companyCode, load, { immediate: true })
</script>

<template>
  <div class="highlight-section">
    <div v-if="loading" class="loading-text">分析中...</div>
    <template v-else-if="items.length > 0">
      <div v-for="item in items" :key="item.id" class="highlight-card">
        <span class="hl-icon">{{ item.icon }}</span>
        <div class="hl-body">
          <div class="hl-title">{{ item.title }}</div>
          <div class="hl-desc">{{ item.description }}</div>
        </div>
      </div>
    </template>
    <div v-else class="empty-text">系统暂未归纳出明确亮点，可结合核心指标和趋势图继续查看。</div>
  </div>
</template>

<style scoped>
.highlight-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-text, .empty-text {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 14px;
}

.highlight-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  background: var(--surface);
  border-left: 4px solid #10b981;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  transition: box-shadow 0.15s;
}

.highlight-card:hover {
  box-shadow: 0 3px 12px rgba(0,0,0,.06);
}

.hl-icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.hl-body {
  flex: 1;
}

.hl-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 4px;
}

.hl-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
