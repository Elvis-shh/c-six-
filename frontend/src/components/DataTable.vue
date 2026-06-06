<script setup lang="ts">
import type { TimelineResponse } from '@/types'

defineProps<{
  timeline: TimelineResponse
  loading?: boolean
}>()
</script>

<template>
  <div class="data-table-wrap">
    <div v-if="loading" class="table-loading">加载中...</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>指标</th>
          <th v-for="year in timeline.years" :key="year">{{ year }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="metric in timeline.metrics" :key="metric.key">
          <td class="metric-name">
            <span class="metric-label">{{ metric.name }}</span>
            <span class="metric-unit">{{ metric.unit }}</span>
          </td>
          <td v-for="(val, i) in metric.values" :key="i">
            <template v-if="val != null">
              {{ metric.unit === '%' ? val.toFixed(1) + '%' : metric.unit === '亿' ? val.toFixed(2) + ' ' + metric.unit : val.toLocaleString('zh-CN') }}
            </template>
            <span v-else class="null-val">—</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.data-table-wrap {
  overflow-x: auto;
  margin-bottom: 32px;
}

.table-loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table thead {
  position: sticky;
  top: 0;
  z-index: 1;
}

.data-table th {
  background: var(--bg-muted, #f5f7fa);
  color: var(--text-secondary);
  font-weight: 600;
  padding: 12px 16px;
  text-align: right;
  border-bottom: 2px solid var(--border, #e8ecf1);
  white-space: nowrap;
}

.data-table th:first-child {
  text-align: left;
  min-width: 140px;
}

.data-table td {
  padding: 12px 16px;
  text-align: right;
  border-bottom: 1px solid var(--border, #e8ecf1);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.data-table td:first-child {
  text-align: left;
}

.data-table tbody tr {
  transition: background 0.15s;
}

.data-table tbody tr:nth-child(even) {
  background: var(--bg-muted, #fafbfc);
}

.data-table tbody tr:hover {
  background: var(--bg-hover, #eef1f6);
}

.metric-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric-label {
  font-weight: 600;
  color: var(--text);
}

.metric-unit {
  font-size: 11px;
  color: var(--text-muted);
}

.null-val {
  color: var(--text-muted);
}
</style>
