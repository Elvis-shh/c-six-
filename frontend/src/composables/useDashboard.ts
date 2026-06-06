// composables/useDashboard.ts — Dashboard 数据加载
import { ref, watch, type Ref } from 'vue'
import { useDashboardStore } from '@/stores'
import { getReportKpi, getReportTimeline } from '@/api'
import type { KpiResponse, TimelineResponse } from '@/types'

export function useDashboard(companyCode: Ref<string>) {
  const store = useDashboardStore()
  const loading = ref(true)
  const error = ref<string | null>(null)
  const kpiData = ref<KpiResponse | null>(null)
  const timelineData = ref<TimelineResponse | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    store.setCompany(companyCode.value)
    try {
      const [kpiRes, timelineRes] = await Promise.all([
        getReportKpi(companyCode.value),
        getReportTimeline(companyCode.value),
      ])
      kpiData.value = kpiRes.data.data
      timelineData.value = timelineRes.data.data
      store.setKpiData(kpiRes.data.data)
    } catch (e: any) {
      const msg = e?.response?.data?.message || '加载失败，请稍后重试'
      error.value = msg
      store.setError(msg)
    } finally {
      loading.value = false
    }
  }

  watch(companyCode, load, { immediate: true })

  return { loading, error, kpiData, timelineData, retry: load }
}
