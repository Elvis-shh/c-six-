<script setup lang="ts">
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import { Chart, registerables } from 'chart.js'
import { getPredict } from '@/api'

Chart.register(...registerables)

const props = defineProps<{ companyCode: string }>()

const loading = ref(false)
const error = ref<string | null>(null)
const predictData = ref<any>(null)
const insights = ref<any>(null)

const canvasRef = ref<HTMLCanvasElement | null>(null)
let chart: Chart | null = null

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
  if (!canvasRef.value || !predictData.value) return
  if (chart) chart.destroy()

  const data = predictData.value
  const colors = ['#3b82f6', '#ef4444', '#3b82f6', '#ef4444', '#3b82f644', '#ef444444']
  const datasets: any[] = []

  const drawnKeys = new Set<string>()
  for (const s of data.series) {
    if (s.key.includes('_upper') || s.key.includes('_lower')) continue
    const baseKey = s.key.replace('_pred', '')
    if (drawnKeys.has(baseKey)) continue
    drawnKeys.add(baseKey)

    // Find actual, predicted, upper, lower
    const actual = data.series.find((x: any) => x.key === baseKey && x.type === 'solid')
    const predicted = data.series.find((x: any) => x.key === baseKey + '_pred')
    const upper = data.series.find((x: any) => x.key === baseKey + '_upper')
    const lower = data.series.find((x: any) => x.key === baseKey + '_lower')

    const colorIdx = datasets.length
    const color = colors[colorIdx % colors.length]

    if (actual) {
      datasets.push({
        label: s.name,
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
        label: s.name + '（预测）',
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
    // Confidence band
    if (upper && lower) {
      datasets.push({
        label: s.name + ' 置信区间',
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
              if (n >= 10000) return (n / 10000).toFixed(1) + '万'
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

onBeforeUnmount(() => {
  if (chart) chart.destroy()
})
</script>

<template>
  <div class="predict-section">
    <div v-if="loading" class="loading-text">预测计算中...</div>
    <div v-else-if="error" class="error-text">{{ error }}</div>
    <template v-else-if="predictData">
      <div class="predict-chart-wrap">
        <canvas ref="canvasRef"></canvas>
      </div>

      <!-- 洞察文案 -->
      <div v-if="insights" class="insights-grid">
        <div v-for="(ins, key) in insights" :key="key" class="insight-card">
          <div class="insight-header">
            <span class="insight-icon">{{ ins.trend === '增长' ? '📈' : '📉' }}</span>
            <span class="insight-title">{{ ins.name }}</span>
          </div>
          <div class="insight-body">
            <p>
              基于近5年数据（R²={{ ins.r2 }}），
              预计 {{ ins.name }} 将保持<strong>{{ ins.trend }}</strong>趋势，
              下一年达到 <strong>{{ ins.predictedValue }} {{ ins.unit }}</strong>，
              同比变化 <strong :class="ins.change > 0 ? 'up' : 'down'">{{ ins.change > 0 ? '+' : '' }}{{ ins.change }}%</strong>。
            </p>
          </div>
        </div>
      </div>
      <div class="disclaimer">
        ⚠️ 以上预测基于历史数据的简单线性回归，仅供参考，不构成投资建议。
      </div>
    </template>
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

.insights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.insight-card {
  padding: 18px 20px;
  background: var(--surface);
  border-radius: 10px;
  border: 1px solid var(--border, #e8ecf1);
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.insight-icon {
  font-size: 20px;
}

.insight-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text);
}

.insight-body p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0;
}

.insight-body strong {
  color: var(--text);
}

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
