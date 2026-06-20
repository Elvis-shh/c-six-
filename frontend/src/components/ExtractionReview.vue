<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { ExtractedIndicator } from '@/types'

const props = defineProps<{ data: Record<string, ExtractedIndicator> }>()
const emit = defineEmits<{ confirm: [data: Record<string, ExtractedIndicator>] }>()
const editable = reactive<Record<string, ExtractedIndicator>>({})
const labels: Record<string, string> = {
  revenue: '营业收入', profit: '净利润', totalAssets: '总资产', totalLiabilities: '总负债', cashFlow: '经营现金流', grossMargin: '毛利率',
}

watch(() => props.data, value => Object.assign(editable, JSON.parse(JSON.stringify(value || {}))), { immediate: true })
</script>

<template>
  <div class="review-card">
    <h3>提取结果确认</h3>
    <table>
      <thead><tr><th>指标</th><th>值</th><th>单位</th><th>置信度</th></tr></thead>
      <tbody>
        <tr v-for="(item, key) in editable" :key="key" :class="{ warn: item.confidence < 0.8 }">
          <td>{{ labels[key] || item.matchedText || key }}</td>
          <td><input v-model.number="item.value" type="number" step="0.01"></td>
          <td>{{ item.unit }}</td>
          <td>{{ Math.round(item.confidence * 100) }}%</td>
        </tr>
      </tbody>
    </table>
    <button @click="emit('confirm', editable)">确认并生成报告</button>
  </div>
</template>

<style scoped>
.review-card {
  margin-top: 16px;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
}
table { width: 100%; margin: 12px 0; border-collapse: collapse; }
th, td { padding: 10px; border-bottom: 1px solid var(--border-light); text-align: left; }
.warn { background: #fffbeb; }
button {
  padding: 10px 16px;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}
</style>
