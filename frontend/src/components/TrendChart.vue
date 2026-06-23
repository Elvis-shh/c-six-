<script setup lang="ts">
import { ref, watch, onBeforeUnmount, computed, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import type { TimelineResponse } from '@/types'
import { cleanIndicatorName } from '@/utils'

Chart.register(...registerables)

const props = defineProps<{
  timeline: TimelineResponse
  loading?: boolean
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

const selectedMetric = ref(0)

const metricOptions = computed(() =>
  props.timeline?.metrics?.map((m, i) => ({ key: m.key, name: m.name, unit: m.unit, index: i })) ?? []
)

const currentMetric = computed(() => props.timeline?.metrics?.[selectedMetric.value])

async function buildChart() {
  await nextTick()
  if (!canvasRef.value || !currentMetric.value) return
  if (chart) chart.destroy()

  const metric = currentMetric.value
  const colors = ['#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6']

  chart = new Chart(canvasRef.value, {
    type: 'line',
    data: {
      labels: props.timeline.years,
      datasets: [{
        label: metric.name,
        data: metric.values.map(v => v != null ? Number(v) : null),
        borderColor: colors[selectedMetric.value % colors.length],
        backgroundColor: colors[selectedMetric.value % colors.length] + '20',
        borderWidth: 2.5,
        pointRadius: 5,
        pointHoverRadius: 7,
        tension: 0.3,
        fill: true,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label(ctx: any) {
              const v = ctx.parsed.y
              if (v == null) return '—'
              if (metric.unit === '%') return Number(v).toFixed(1) + '%'
              if (metric.unit === '亿') return Number(v).toFixed(2) + ' 亿'
              if (metric.unit === '万') return Number(v).toFixed(2) + ' 万'
              return Number(v).toLocaleString('zh-CN')
            },
          },
        },
      },
      scales: {
        x: {
          title: {
            display: true,
            text: '年度',
          },
        },
        y: {
          title: {
            display: true,
            text: metric.unit ? `${metric.name}（${metric.unit}）` : metric.name,
          },
          ticks: {
            callback(v) {
              const n = Number(v)
              if (metric.unit === '%') return n.toFixed(0) + '%'
              if (metric.unit === '亿') {
                if (Math.abs(n) >= 10000) return (n / 10000).toFixed(1) + '万亿'
                return n.toFixed(1) + ' 亿'
              }
              if (Math.abs(n) >= 10000) return (n / 10000).toFixed(1) + '万'
              return n
            },
          },
        },
      },
    },
  })
}

watch(() => props.timeline, buildChart, { immediate: true })
watch(selectedMetric, buildChart)

onBeforeUnmount(() => {
  if (chart) chart.destroy()
})
</script>

<template>
  <div class="chart-section">
    <div v-if="loading" class="chart-loading">加载中...</div>
    <template v-else-if="timeline">
      <div class="chart-tabs">
        <button
          v-for="opt in metricOptions"
          :key="opt.key"
          class="chart-tab"
          :class="{ active: opt.index === selectedMetric }"
          @click="selectedMetric = opt.index"
        >
          {{ cleanIndicatorName(opt.name) }}<span v-if="opt.unit">（{{ opt.unit }}）</span>
        </button>
      </div>
      <div class="chart-container">
        <canvas ref="canvasRef"></canvas>
      </div>
    </template>
  </div>
</template>

<style scoped>
.chart-section {
  margin-bottom: 32px;
}

.chart-loading {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.chart-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.chart-tab {
  padding: 8px 18px;
  border: 1px solid var(--border, #e8ecf1);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.chart-tab:hover {
  border-color: var(--primary, #3b82f6);
  color: var(--primary, #3b82f6);
}

.chart-tab.active {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.chart-container {
  position: relative;
  height: 320px;
  background: var(--surface);
  border-radius: 12px;
  padding: 16px;
  border: 1px solid var(--border, #e8ecf1);
}
</style>
