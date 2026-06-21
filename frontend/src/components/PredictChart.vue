<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { getPredict } from '@/api'
import InsightPanel from '@/components/InsightPanel.vue'
import type { InsightData } from '@/components/InsightPanel.vue'

Chart.register(...registerables)

const props = defineProps<{ companyCode: string }>()

const loading = ref(false)
const error = ref<string | null>(null)
const predictData = ref<any>(null)
const insights = ref<any>(null)
const fullInsights = ref<InsightData | null>(null)
const selectedMetric = ref(0)

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

const metricOptions = computed(() =>
  Object.entries(insights.value || {}).map(([key, item], index) => ({ key, name: (item as any).name, index }))
)

const currentMetricKey = computed(() => metricOptions.value[selectedMetric.value]?.key)
const currentInsight = computed(() => currentMetricKey.value ? insights.value?.[currentMetricKey.value] : null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await getPredict(props.companyCode)
    predictData.value = res.data.data
    insights.value = res.data.data?.insights
    fullInsights.value = res.data.data?.fullInsights || null
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载预测数据失败'
  } finally {
    loading.value = false
  }
}

async function buildChart() {
  await nextTick()
  if (!canvasRef.value || !predictData.value || !currentMetricKey.value) return
  if (chart) chart.destroy()

  const data = predictData.value
  const colors = ['#3b82f6', '#ef4444', '#10b981', '#f59e0b', '#8b5cf6']
  const datasets: any[] = []
  const baseKey = currentMetricKey.value
  const currentName = currentInsight.value?.name || baseKey
  const color = colors[selectedMetric.value % colors.length]
  const actual = data.series.find((x: any) => x.key === baseKey && x.type === 'solid')
  const predicted = data.series.find((x: any) => x.key === baseKey + '_pred')
  const upper = data.series.find((x: any) => x.key === baseKey + '_upper')
  const lower = data.series.find((x: any) => x.key === baseKey + '_lower')

  if (actual) {
    datasets.push({
      label: currentName,
      data: actual.values,
      borderColor: color,
      backgroundColor: color + '20',
      borderWidth: 3,
      pointRadius: 4,
      tension: 0.2,
      spanGaps: false,
    })
  }
  if (predicted) {
    datasets.push({
      label: currentName + '（预测）',
      data: predicted.values,
      borderColor: color,
      borderDash: [6, 3],
      borderWidth: 2.5,
      pointRadius: 4,
      pointStyle: 'rectRounded',
      tension: 0.2,
      fill: false,
    })
  }
  if (upper && lower) {
    datasets.push({
      label: currentName + ' 置信区间',
      data: upper.values,
      borderColor: 'transparent',
      backgroundColor: color + '18',
      fill: {
        target: { values: lower.values },
        above: color + '18',
      },
      pointRadius: 0,
      tension: 0.2,
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
          labels: {
            usePointStyle: true,
            padding: 20,
            filter: (item) => !item.text.includes('置信区间'),
          },
        },
        tooltip: {
          callbacks: {
            label(ctx: any) {
              const v = ctx.parsed.y
              if (v == null) return '—'
              const seriesName = ctx.dataset.label || ''
              const insight = Object.values(insights.value || {}).find((item: any) => seriesName.includes(item.name)) as any
              const unit = insight?.unit || ''
              if (unit === '%') return `${Number(v).toFixed(1)}%`
              if (unit === '亿') return `${Number(v).toFixed(2)} 亿`
              return Number(v).toLocaleString('zh-CN')
            },
          },
        },
      },
      scales: {
        y: {
          ticks: {
            callback(v) {
              const n = Number(v)
              if (Math.abs(n) >= 10000) return (n / 10000).toFixed(1) + '万'
              return n
            },
          },
        },
      },
    },
  })
}

watch(() => props.companyCode, load, { immediate: true })
watch(predictData, buildChart)
watch(selectedMetric, buildChart)

onBeforeUnmount(() => {
  if (chart) chart.destroy()
})
</script>

<template>
  <div class="predict-section">
    <div v-if="loading" class="loading-text">预测计算中...</div>
    <div v-else-if="error" class="error-text">{{ error }}</div>
    <template v-else-if="predictData && predictData.series?.length">
      <div class="chart-tabs">
        <button
          v-for="opt in metricOptions"
          :key="opt.key"
          class="chart-tab"
          :class="{ active: opt.index === selectedMetric }"
          @click="selectedMetric = opt.index"
        >
          {{ opt.name }}
        </button>
      </div>
      <div class="predict-chart-wrap">
        <canvas ref="canvasRef"></canvas>
      </div>

      <!-- Story 5.2: 预测洞察面板（4 段文案 + 免责声明） -->
      <InsightPanel :insights="fullInsights" />
    </template>
    <div v-else class="loading-text">可用于预测的连续历史指标不足</div>
  </div>
</template>

<style scoped>
.predict-section {
  margin-bottom: 40px;
}

.loading-text, .error-text {
  text-align: center;
  padding: 40px;
  color: var(--text-muted);
}

.error-text {
  color: #ef4444;
}

.predict-chart-wrap {
  position: relative;
  height: 380px;
  background: var(--surface);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid var(--border, #e8ecf1);
  margin-bottom: 24px;
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


</style>
