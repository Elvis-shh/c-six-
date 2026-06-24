<script setup lang="ts">
import type { TimelineResponse } from '@/types'
import { cleanIndicatorName } from '@/utils'

const indicatorTips: Record<string, string> = {
  revenue: '营业收入，反映公司主营业务整体进账规模。',
  profit: '归母净利润，反映真正归属于股东的盈利水平。',
  cashFlow: '经营现金流净额，反映公司主营业务实际回笼现金能力。',
  grossMargin: '毛利率，反映产品或服务的基础赚钱能力。',
  netMargin: '净利率，反映收入最终转化成净利润的比例。',
  debtRatio: '资产负债率，反映总资产中有多少比例来自负债融资。',
  roe: '净资产收益率，反映股东投入资本的回报效率。',
  eps: '每股收益，反映每一股普通股对应的盈利水平。',
  rdExpenseRatio: '研发费用率，反映研发投入占收入的比例。',
  totalAssets: '总资产，反映公司拥有和控制的全部资源规模。',
  totalLiabilities: '总负债，反映公司需要偿还的全部债务规模。',
}

function tipFor(metric: TimelineResponse['metrics'][number]) {
  return indicatorTips[metric.key] || ''
}

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
            <span class="metric-label">
              {{ cleanIndicatorName(metric.name) }}
              <span v-if="tipFor(metric)" class="metric-tooltip" :data-tip="tipFor(metric)">?</span>
            </span>
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.metric-tooltip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--bg-muted, #e8ecf1);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 700;
  cursor: help;
  position: relative;
}

.metric-tooltip:hover::after,
.metric-tooltip:focus::after {
  content: attr(data-tip);
  position: absolute;
  left: 50%;
  bottom: calc(100% + 8px);
  transform: translateX(-50%);
  background: #1e293b;
  color: #f1f5f9;
  font-size: 12px;
  font-weight: 400;
  white-space: pre-wrap;
  max-width: 320px;
  width: max-content;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  z-index: 100;
  pointer-events: none;
}

.metric-tooltip:hover::before,
.metric-tooltip:focus::before {
  content: '';
  position: absolute;
  left: 50%;
  bottom: calc(100% + 2px);
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: #1e293b;
  z-index: 100;
  pointer-events: none;
}

.metric-unit {
  font-size: 11px;
  color: var(--text-muted);
}

.null-val {
  color: var(--text-muted);
}
</style>
