<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useDashboard } from '@/composables/useDashboard'
import { useHistory } from '@/composables/useHistory'
import KpiCards from '@/components/KpiCards.vue'
import DataTable from '@/components/DataTable.vue'
import TrendChart from '@/components/TrendChart.vue'
import IndicatorTable from '@/components/IndicatorTable.vue'
import HighlightCards from '@/components/HighlightCards.vue'
import RiskCards from '@/components/RiskCards.vue'
import PredictChart from '@/components/PredictChart.vue'
import ChatPanel from '@/components/ChatPanel.vue'
import ChatToggle from '@/components/ChatToggle.vue'
import FileUpload from '@/components/FileUpload.vue'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorState from '@/components/ErrorState.vue'
import type { Ref } from 'vue'

const route = useRoute()
const code = computed(() => route.params.code as string)
const { loading, error, kpiData, timelineData, retry } = useDashboard(code as Ref<string>)
const history = useHistory()

watch(kpiData, value => {
  if (value?.company) {
    history.add({
      code: value.company.code,
      name: value.company.name,
      reportYear: value.reportYear,
    })
  }
}, { immediate: true })
</script>

<template>
  <div class="dashboard-page container">
    <!-- 加载态 -->
    <div v-if="loading" class="skeleton-grid">
      <SkeletonCard v-for="i in 6" :key="i" />
    </div>

    <!-- 错误态 -->
    <ErrorState v-else-if="error" :message="error" @retry="retry" />

    <!-- 正常渲染 -->
    <template v-else-if="kpiData">
      <div class="dashboard-header">
        <div>
          <h1>{{ kpiData.company.name }}</h1>
          <span class="header-code">{{ kpiData.company.code }}</span>
          <span class="header-year">{{ kpiData.reportYear }} 年报</span>
        </div>
        <FileUpload class="header-upload" :company-code="code" :company-name="kpiData.company.name" />
      </div>

      <!-- Story 3.1: KPI 卡片 -->
      <section class="dashboard-section">
        <h2>📊 核心指标概览</h2>
        <KpiCards :kpis="kpiData.kpis" />
      </section>

      <!-- Story 3.2: 五年数据表格 -->
      <section v-if="timelineData" class="dashboard-section">
        <h2>📋 近五年关键数据</h2>
        <DataTable :timeline="timelineData" />
      </section>

      <!-- Story 3.3: 折线图趋势 -->
      <section v-if="timelineData" class="dashboard-section">
        <h2>📈 趋势图表</h2>
        <TrendChart :timeline="timelineData" />
      </section>

      <!-- Story 3.4: 核心指标详解 -->
      <section class="dashboard-section">
        <h2>🔍 核心指标详解</h2>
        <IndicatorTable :company-code="code" :max-sort-order="10" />
      </section>

      <!-- 其他指标分析 -->
      <section class="dashboard-section">
        <h2>📝 其他指标分析</h2>
        <IndicatorTable :company-code="code" :min-sort-order="11" />
      </section>

      <!-- Story 4.1 & 4.2: 亮点与风险 -->
      <section class="dashboard-section">
        <div class="hl-risk-grid">
          <div class="hl-risk-col">
            <h2>✨ 经营亮点</h2>
            <HighlightCards :company-code="code" />
          </div>
          <div class="hl-risk-col">
            <h2>⚠️ 风险提示</h2>
            <RiskCards :company-code="code" />
          </div>
        </div>
      </section>

      <!-- Story 5.1 & 5.2: 趋势预测 -->
      <section class="dashboard-section">
        <h2>🔮 趋势预测</h2>
        <PredictChart :company-code="code" />
      </section>

      <ChatToggle />
      <ChatPanel :company-code="code" :company-name="kpiData.company.name" />
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  padding: 32px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 32px;
}
.dashboard-header h1 {
  font-size: 26px;
  font-weight: 700;
  margin: 0;
}
.header-code {
  font-size: 14px;
  color: var(--text-muted);
  font-family: monospace;
  background: var(--surface-alt, #f5f7fa);
  padding: 2px 8px;
  border-radius: 4px;
}
.header-year {
  font-size: 13px;
  color: var(--primary, #3b82f6);
  background: var(--primary-light, #eff6ff);
  padding: 2px 10px;
  border-radius: 4px;
}
.header-upload {
  width: min(360px, 42vw);
}

.dashboard-section {
  margin-bottom: 40px;
}
.dashboard-section h2 {
  font-size: 18px;
  margin-bottom: 20px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  padding-top: 32px;
}

.hl-risk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 24px;
}

.hl-risk-col h2 {
  font-size: 18px;
  margin-bottom: 16px;
}

@media (max-width: 760px) {
  .dashboard-header {
    display: block;
  }
  .header-upload {
    width: 100%;
    margin-top: 16px;
  }
}
</style>
