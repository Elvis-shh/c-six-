<script setup lang="ts">
import { ref, watch } from 'vue'
import { getRisks } from '@/api'
import type { RiskItem } from '@/types'

const props = defineProps<{ companyCode: string }>()

const loading = ref(false)
const items = ref<RiskItem[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getRisks(props.companyCode)
    items.value = res.data.data ?? []
  } finally {
    loading.value = false
  }
}

watch(() => props.companyCode, load, { immediate: true })
</script>

<template>
  <div class="risk-section">
    <div v-if="loading" class="loading-text">分析中...</div>
    <template v-else-if="items.length > 0">
      <div v-for="item in items" :key="item.id" class="risk-card">
        <span class="risk-icon">{{ item.icon }}</span>
        <div class="risk-body">
          <div class="risk-title">{{ item.title }}</div>
          <div class="risk-desc">{{ item.description }}</div>
        </div>
      </div>
    </template>
    <div v-else class="safe-text">系统暂未识别出明确风险项，建议继续关注现金流、利润和负债变化。</div>
  </div>
</template>

<style scoped>
.risk-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-text, .safe-text {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 14px;
}

.safe-text {
  color: #10b981;
  font-weight: 600;
}

.risk-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  background: var(--surface);
  border-left: 4px solid #ef4444;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  transition: box-shadow 0.15s;
}

.risk-card:hover {
  box-shadow: 0 3px 12px rgba(0,0,0,.06);
}

.risk-icon {
  font-size: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.risk-body {
  flex: 1;
}

.risk-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 4px;
}

.risk-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
}
</style>
