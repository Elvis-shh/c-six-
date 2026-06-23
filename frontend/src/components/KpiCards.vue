<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import type { KpiItem } from '@/types'
import { cleanIndicatorName } from '@/utils'

const props = defineProps<{ kpis: KpiItem[] }>()

const animatedVals = ref<Record<string, number>>({})
const rafIds: Record<string, number> = {}

watch(
  () => props.kpis,
  (kpis) => {
    for (const k of kpis) {
      animateValue(k.key, k.value)
    }
  },
  { immediate: true }
)

function animateValue(key: string, target: number, duration = 800) {
  if (rafIds[key]) cancelAnimationFrame(rafIds[key])
  const start = animatedVals.value[key] ?? 0
  const startTime = performance.now()

  function step(now: number) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animatedVals.value = { ...animatedVals.value, [key]: start + (target - start) * eased }
    if (progress < 1) {
      rafIds[key] = requestAnimationFrame(step)
    }
  }
  rafIds[key] = requestAnimationFrame(step)
}

onBeforeUnmount(() => {
  for (const id of Object.values(rafIds)) cancelAnimationFrame(id)
})

function formatValue(kpi: KpiItem): string {
  const val = animatedVals.value[kpi.key] ?? kpi.value
  if (kpi.unit === '%') return val.toFixed(1) + '%'
  if (kpi.unit === '亿') return val.toLocaleString('zh-CN') + ' 亿'
  return val.toLocaleString('zh-CN')
}

function trendArrow(trend: string, yoy: number | null): string {
  if (yoy == null) return '—'
  if (trend === 'up') return `↑ ${Math.abs(yoy).toFixed(1)}%`
  if (trend === 'down') return `↓ ${Math.abs(yoy).toFixed(1)}%`
  if (trend === 'down_good') return `↓ ${Math.abs(yoy).toFixed(1)}%`
  return '—'
}

function trendClass(trend: string): string {
  if (trend === 'up' || trend === 'down_good') return 'trend-up'
  if (trend === 'down') return 'trend-down'
  return 'trend-neutral'
}

const iconMap: Record<string, string> = {
  revenue: '💰',
  profit: '📈',
  debtRatio: '⚖️',
  cashFlow: '💵',
}
</script>

<template>
  <div class="kpi-grid">
    <div v-for="kpi in kpis" :key="kpi.key" class="kpi-card">
      <div class="kpi-header">
        <span class="kpi-icon">{{ iconMap[kpi.key] || '📊' }}</span>
        <span class="kpi-name">{{ cleanIndicatorName(kpi.name) }}<span v-if="kpi.unit" class="kpi-unit">（{{ kpi.unit }}）</span><span v-if="kpi.explanation" class="kpi-tooltip" :data-tip="kpi.explanation">?</span></span>
      </div>
      <div class="kpi-value">{{ formatValue(kpi) }}</div>
      <div class="kpi-footer">
        <span class="kpi-yoy-label">同比</span>
        <span class="kpi-change" :class="trendClass(kpi.trend)">
          {{ trendArrow(kpi.trend, kpi.yoy) }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.kpi-card {
  padding: 28px 24px;
  background: var(--surface);
  border-radius: 14px;
  border: 1px solid var(--border, #e8ecf1);
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
  transition: transform 0.2s, box-shadow 0.2s;
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,.08);
}

.kpi-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.kpi-icon {
  font-size: 18px;
}

.kpi-name {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.kpi-unit {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-muted);
}

.kpi-tooltip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--bg-muted, #e8ecf1);
  color: var(--text-secondary);
  font-size: 10px;
  font-weight: 700;
  cursor: help;
  position: relative;
  flex-shrink: 0;
}
.kpi-tooltip:hover::after,
.kpi-tooltip:focus::after {
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
  max-width: 300px;
  width: max-content;
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
  z-index: 100;
  pointer-events: none;
}
.kpi-tooltip:hover::before,
.kpi-tooltip:focus::before {
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

.kpi-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 12px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;
}

.kpi-footer {
  display: flex;
  align-items: center;
  gap: 6px;
}

.kpi-yoy-label {
  font-size: 12px;
  color: var(--text-muted);
}

.kpi-change {
  font-size: 14px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
}

.trend-up {
  color: #fff;
  background: #e53e3e;
}

.trend-down {
  color: #fff;
  background: #38a169;
}

.trend-neutral {
  color: var(--text-muted);
  background: var(--bg-muted, #f0f0f0);
}
</style>
