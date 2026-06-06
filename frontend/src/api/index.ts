// api/index.ts — SmartReport API 请求层
import axios from 'axios'
import type { ApiResponse, Company, KpiResponse, TimelineResponse, BenchmarkResponse, HighlightItem, RiskItem, PredictResponse } from '@/types'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// ============================================================
// 搜索模块
// ============================================================
export const searchCompanies = (q: string, limit = 8) =>
  api.get<ApiResponse<Company[]>>('/search/companies', { params: { q, limit } })

export const getHotCompanies = () =>
  api.get<ApiResponse<Company[]>>('/search/companies/hot')

// ============================================================
// 财报数据模块
// ============================================================
export const getReportKpi = (companyCode: string) =>
  api.get<ApiResponse<KpiResponse>>(`/reports/${companyCode}/kpi`)

export const getReportTimeline = (companyCode: string, metrics?: string) =>
  api.get<ApiResponse<TimelineResponse>>(`/reports/${companyCode}/timeline`, { params: { metrics } })

export const getReportLatest = (companyCode: string) =>
  api.get<ApiResponse<KpiResponse>>(`/reports/${companyCode}/latest`)

export const getReportIndicators = (companyCode: string) =>
  api.get<ApiResponse<any>>(`/reports/${companyCode}/indicators`)

// ============================================================
// 分析模块
// ============================================================
export const getHighlights = (companyCode: string) =>
  api.get<ApiResponse<HighlightItem[]>>(`/analysis/${companyCode}/highlights`)

export const getRisks = (companyCode: string) =>
  api.get<ApiResponse<RiskItem[]>>(`/analysis/${companyCode}/risks`)

export const getPredict = (companyCode: string) =>
  api.get<ApiResponse<PredictResponse>>(`/analysis/${companyCode}/predict`)

export const getBenchmark = (companyCode: string, year?: number) =>
  api.get<ApiResponse<BenchmarkResponse>>(`/analysis/${companyCode}/benchmark`, { params: { year } })

export const getTerms = () =>
  api.get<ApiResponse<Record<string, string>>>('/terms')

export default api

