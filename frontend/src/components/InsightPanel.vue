<script setup lang="ts">
import Disclaimer from '@/components/Disclaimer.vue'

export interface InsightData {
  companyName?: string
  revenueTrend?: {
    title: string
    description: string
  }
  profitOutlook?: {
    title: string
    description: string
  }
  assumptions?: string[]
  riskNote?: string
}

defineProps<{
  insights: InsightData | null
  loading?: boolean
}>()
</script>

<template>
  <div class="insight-panel">
    <!-- 加载态 -->
    <div v-if="loading" class="insight-loading">
      <div class="skeleton-line" v-for="i in 4" :key="i" />
    </div>

    <!-- 空态 -->
    <div v-else-if="!insights || !insights.revenueTrend" class="insight-empty">
      📝 暂无可用的预测洞察数据
    </div>

    <!-- 正常渲染 4 段文案 -->
    <template v-else>
      <h3 class="insight-panel-title">📝 预测洞察</h3>

      <!-- 1. 营收趋势 -->
      <div class="insight-card">
        <div class="insight-card-header">
          <span class="insight-card-icon">📈</span>
          <span class="insight-card-title">{{ insights.revenueTrend?.title || '营收趋势' }}</span>
        </div>
        <p class="insight-card-body">{{ insights.revenueTrend?.description }}</p>
      </div>

      <!-- 2. 盈利能力预测 -->
      <div class="insight-card">
        <div class="insight-card-header">
          <span class="insight-card-icon">💰</span>
          <span class="insight-card-title">{{ insights.profitOutlook?.title || '盈利能力预测' }}</span>
        </div>
        <p class="insight-card-body">{{ insights.profitOutlook?.description }}</p>
      </div>

      <!-- 3. 关键假设 -->
      <div class="insight-card">
        <div class="insight-card-header">
          <span class="insight-card-icon">🔑</span>
          <span class="insight-card-title">关键假设</span>
        </div>
        <ul class="insight-assumptions">
          <li v-for="(item, idx) in insights.assumptions" :key="idx">{{ item }}</li>
        </ul>
      </div>

      <!-- 4. 风险提示 -->
      <div class="insight-card insight-card--risk">
        <div class="insight-card-header">
          <span class="insight-card-icon">⚠️</span>
          <span class="insight-card-title">风险提示</span>
        </div>
        <p class="insight-card-body">{{ insights.riskNote }}</p>
      </div>

      <!-- 免责声明 -->
      <Disclaimer />
    </template>
  </div>
</template>

<style scoped>
.insight-panel {
  margin-bottom: 32px;
}

.insight-panel-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text, #1f2937);
  margin: 0 0 16px 0;
}

.insight-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f3f4f6 25%, #e5e7eb 50%, #f3f4f6 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.insight-empty {
  text-align: center;
  color: var(--text-secondary, #9ca3af);
  padding: 32px 0;
  font-size: 14px;
}

.insight-card {
  padding: 16px;
  background: var(--surface, #ffffff);
  border: 1px solid var(--border, #e8ecf1);
  border-radius: 10px;
  margin-bottom: 12px;
  transition: box-shadow 0.15s;
}

.insight-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.insight-card--risk {
  background: #fff7ed;
  border-color: #fed7aa;
}

.insight-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.insight-card-icon {
  font-size: 16px;
}

.insight-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text, #1f2937);
}

.insight-card-body {
  font-size: 14px;
  color: var(--text-secondary, #4b5563);
  line-height: 1.65;
  margin: 0;
}

.insight-assumptions {
  margin: 0;
  padding-left: 20px;
  font-size: 14px;
  color: var(--text-secondary, #4b5563);
  line-height: 1.7;
}

.insight-assumptions li {
  margin-bottom: 2px;
}
</style>
