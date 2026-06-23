<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { getPredict } from '@/api'
import { cleanIndicatorName } from '@/utils'

Chart.register(...registerables)

const props = defineProps<{ companyCode: string }>()

const loading = ref(false)
const error = ref<string | null>(null)
const predictData = ref<any>(null)
const insights = ref<any>(null)

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

const selectedIndex = ref(0)

const metricOptions = computed(() =>
  Object.entries(insights.value || {}).map(([key, item], index) => ({
    key,
    name: (item as any).name,
    unit: (item as any).unit,
    index,
  }))
)

const currentMetric = computed(() => metricOptions.value[selectedIndex.value])
const currentInsight = computed(() => insights.value?.[currentMetric.value?.key])

function formatYAxisTick(n: number, unit: string): string {
  if (unit === '%') return n.toFixed(0) + '%'
  if (Math.abs(n) >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (Math.abs(n) >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function formatTooltip(v: number, unit: string): string {
  if (v == null) return '—'
  if (unit === '%') return Number(v).toFixed(1) + '%'
  if (unit === '亿') return Number(v).toFixed(2) + ' 亿'
  if (unit === '万') return Number(v).toFixed(2) + ' 万'
  return Number(v).toLocaleString('zh-CN')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await getPredict(props.companyCode)
    predictData.value = res.data.data
    insights.value = res.data.data?.insights
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载预测数据失败'
  } finally {
    loading.value = false
  }
}

async function buildChart() {
  await nextTick()
  if (!canvasRef.value || !predictData.value || !currentMetric.value) return
  if (chart) chart.destroy()

  const metric = currentMetric.value
  const data = predictData.value
  const color = '#3b82f6'
  const datasets: any[] = []

  const actual = data.series.find((x: any) => x.key === metric.key && x.type === 'solid')
  const predicted = data.series.find((x: any) => x.key === metric.key + '_pred')

  const displayName = metric.unit ? `${metric.name}（${metric.unit}）` : metric.name
  if (actual) {
    datasets.push({
      label: displayName,
      data: actual.values,
      borderColor: color,
      backgroundColor: color + '18',
      borderWidth: 2.5,
      pointRadius: 4,
      tension: 0.2,
      spanGaps: false,
    })
  }
  if (predicted) {
    datasets.push({
      label: displayName + '（预测）',
      data: predicted.values,
      borderColor: '#f59e0b',
      borderDash: [6, 4],
      borderWidth: 2,
      pointRadius: 3,
      tension: 0.2,
      fill: false,
    })
  }

  chart = new Chart(canvasRef.value, {
    type: 'line',
    data: { labels: data.years, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom',
          labels: { usePointStyle: true, padding: 20 },
        },
        tooltip: {
          callbacks: {
            label(ctx: any) {
              const v = ctx.parsed.y
              if (v == null) return '—'
              return ctx.dataset.label + ': ' + formatTooltip(v, metric.unit)
            },
          },
        },
      },
      scales: {
        x: {
          title: { display: true, text: '年度' },
        },
        y: {
          title: {
            display: true,
            text: metric.unit ? `${metric.name}（${metric.unit}）` : metric.name,
          },
          ticks: {
            callback(v: any) { return formatYAxisTick(Number(v), metric.unit) },
          },
        },
      },
    },
  })
}

watch(() => props.companyCode, load, { immediate: true })
watch(predictData, buildChart)
watch(selectedIndex, buildChart)

onBeforeUnmount(() => {
  if (chart) chart.destroy()
})
</script>

<template>
  <div class="predict-section">
    <div v-if="loading" class="loading-text">预测计算中...</div>
    <div v-else-if="error" class="error-text">{{ error }}</div>
    <template v-else-if="predictData && metricOptions.length">
      <div class="predict-tabs">
        <button
          v-for="opt in metricOptions"
          :key="opt.key"
          class="predict-tab"
          :class="{ active: opt.index === selectedIndex }"
          @click="selectedIndex = opt.index"
        >
          {{ cleanIndicatorName(opt.name) }}<span v-if="opt.unit">（{{ opt.unit }}）</span>
        </button>
      </div>

      <div class="predict-chart-wrap">
        <canvas ref="canvasRef"></canvas>
      </div>

      <div v-if="currentInsight" class="insight-card">
        <div class="insight-header">
          <span class="insight-icon">{{ currentInsight.trend === '增长' ? '📈' : '📉' }}</span>
          <span class="insight-title">{{ cleanIndicatorName(currentInsight.name) }}</span>
        </div>
        <div class="insight-body">
          <p>
            基于近年可用财报数据（R²={{ currentInsight.r2 }}），
            预计 {{ currentInsight.name }} 将保持<strong>{{ currentInsight.trend }}</strong>趋势，
            下一年达到 <strong>{{ currentInsight.predictedValue }} {{ currentInsight.unit }}</strong>，
            同比变化 <strong :class="currentInsight.change > 0 ? 'up' : 'down'">{{ currentInsight.change > 0 ? '+' : '' }}{{ currentInsight.change }}%</strong>。
          </p>
        </div>
      </div>

      <div class="disclaimer">
        ⚠️ 以上预测基于历史数据的简单线性回归，仅供参考，不构成投资建议。
      </div>
    </template>
    <div v-else class="loading-text">可用于预测的连续历史指标不足</div>
  </div>
</template>

<style scoped>
.predict-section { margin-bottom: 40px; }
.loading-text, .error-text { text-align: center; padding: 40px; color: var(--text-muted); }
.error-text { color: #ef4444; }

.predict-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.predict-tab {
  padding: 8px 16px;
  border: 1px solid var(--border, #e8ecf1);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.predict-tab:hover { border-color: var(--primary, #3b82f6); color: var(--primary, #3b82f6); }
.predict-tab.active { background: var(--primary, #3b82f6); color: #fff; border-color: var(--primary, #3b82f6); }

.predict-chart-wrap {
  position: relative;
  height: 380px;
  background: var(--surface);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid var(--border, #e8ecf1);
  margin-bottom: 16px;
}

.insight-card {
  padding: 18px 20px;
  background: var(--surface);
  border-radius: 10px;
  border: 1px solid var(--border, #e8ecf1);
  margin-bottom: 12px;
}
.insight-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.insight-icon { font-size: 20px; }
.insight-title { font-size: 15px; font-weight: 700; color: var(--text); }
.insight-body p { font-size: 13px; color: var(--text-secondary); line-height: 1.7; margin: 0; }
.insight-body strong { color: var(--text); }
.up { color: #e53e3e; }
.down { color: #38a169; }

.disclaimer {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  padding: 12px;
  background: var(--bg-muted, #fff8e1);
  border-radius: 8px;
}
</style>
