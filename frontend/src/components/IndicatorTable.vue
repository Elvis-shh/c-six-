<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { IndicatorDetail } from '@/types'
import { getReportIndicators } from '@/api'
import { cleanIndicatorName } from '@/utils'

const props = defineProps<{ companyCode: string; maxSortOrder?: number; minSortOrder?: number }>()

const loading = ref(false)
const indicators = ref<IndicatorDetail[]>([])

async function load() {
  loading.value = true
  try {
    const res = await getReportIndicators(props.companyCode)
    indicators.value = res.data.data?.indicators ?? []
  } finally {
    loading.value = false
  }
}

watch(() => props.companyCode, load, { immediate: true })

const categoryNames: Record<string, string> = {
  revenue: '收入',
  profit: '利润',
  cashflow: '现金流',
  debt: '偿债',
  ratio: '比率',
  expense: '费用',
  efficiency: '效率',
  growth: '成长',
  valuation: '估值',
}

function formatValue(val: number, unit: string): string {
  if (unit === '%') return val.toFixed(2) + '%'
  if (unit === '亿') return val.toLocaleString('zh-CN') + ' 亿'
  return val.toLocaleString('zh-CN')
}

function yoyText(yoy: number | null, trend: string): string {
  if (yoy == null) return '—'
  const arrow = trend === 'up' || trend === 'down_good' ? '↑' : '↓'
  return `${arrow} ${Math.abs(yoy).toFixed(1)}%`
}

function yoyClass(trend: string): string {
  if (trend === 'up' || trend === 'down_good') return 'yoy-up'
  if (trend === 'down') return 'yoy-down'
  return 'yoy-neutral'
}

function evalClass(evaluation: string): string {
  if (evaluation === '优秀') return 'eval-good'
  if (evaluation === '良好' || evaluation === '正常') return 'eval-ok'
  if (evaluation === '关注' || evaluation === '偏高') return 'eval-warn'
  return 'eval-neutral'
}

const filteredIndicators = computed(() => {
  let list = indicators.value
  if (props.maxSortOrder != null) list = list.filter(i => (i.sortOrder ?? 99) <= props.maxSortOrder!)
  if (props.minSortOrder != null) list = list.filter(i => (i.sortOrder ?? 0) >= props.minSortOrder!)
  return list
})

const groupedByCategory = computed(() => {
  const groups: Record<string, IndicatorDetail[]> = {}
  for (const ind of filteredIndicators.value) {
    const cat = ind.category || 'other'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(ind)
  }
  return groups
})
</script>

<template>
  <div class="indicator-detail-section">
    <div v-if="loading" class="loading-text">加载中...</div>
    <template v-else>
      <div v-for="(items, cat) in groupedByCategory" :key="cat" class="indicator-group">
        <h3 class="group-title">{{ categoryNames[cat] || cat }}</h3>
        <div class="indicator-cards">
          <div v-for="ind in items" :key="ind.key" class="indicator-card">
            <div class="ind-row">
              <span class="ind-name">
                {{ cleanIndicatorName(ind.name) }}<span v-if="ind.unit" class="ind-unit">（{{ ind.unit }}）</span>
                <span v-if="ind.explanation" class="ind-tooltip" :data-tip="ind.explanation">?</span>
              </span>
              <span class="ind-eval" :class="evalClass(ind.evaluation)">{{ ind.evaluation }}</span>
            </div>
            <div class="ind-row">
              <span class="ind-value">{{ formatValue(ind.value, ind.unit) }}</span>
              <span class="ind-yoy" :class="yoyClass(ind.trend)">
                {{ yoyText(ind.yoy, ind.trend) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.indicator-detail-section {
  margin-bottom: 40px;
}

.loading-text {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.indicator-group {
  margin-bottom: 24px;
}

.group-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--primary, #3b82f6);
}

.indicator-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.indicator-card {
  padding: 16px 18px;
  background: var(--surface);
  border-radius: 10px;
  border: 1px solid var(--border, #e8ecf1);
  transition: box-shadow 0.15s;
}

.indicator-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
}

.ind-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ind-row:first-child {
  margin-bottom: 8px;
}

.ind-name {
  font-size: 14px;
  color: var(--text);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ind-tooltip {
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
.ind-tooltip:hover::after,
.ind-tooltip:focus::after {
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
.ind-tooltip:hover::before,
.ind-tooltip:focus::before {
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

.ind-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

.ind-eval {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 6px;
}

.eval-good { background: #dcfce7; color: #166534; }
.eval-ok { background: #dbeafe; color: #1e40af; }
.eval-warn { background: #fef3c7; color: #92400e; }
.eval-neutral { background: #f3f4f6; color: #6b7280; }

.ind-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}

.ind-yoy {
  font-size: 13px;
  font-weight: 600;
}

.yoy-up { color: #e53e3e; }
.yoy-down { color: #38a169; }
.yoy-neutral { color: var(--text-muted); }
</style>
