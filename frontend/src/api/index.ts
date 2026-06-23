// api/index.ts — SmartReport API 请求层
import axios from 'axios'
import type { ApiResponse, Company, KpiResponse, TimelineResponse, BenchmarkResponse, HighlightItem, RiskItem, PredictResponse, UploadTaskResponse, UploadTaskStatus, ExtractedIndicator } from '@/types'

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

// ============================================================
// Phase 3: AI 问答与上传解析
// ============================================================
export const sendChatMessage = (payload: { companyCode: string; message: string; sessionId: string }) =>
  fetch('/api/v1/chat/messages', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })

export const uploadReport = (file: File, onUploadProgress?: (percent: number) => void) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post<ApiResponse<UploadTaskResponse>>('/upload/report', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
    onUploadProgress: event => {
      if (event.total && onUploadProgress) onUploadProgress(Math.round((event.loaded / event.total) * 100))
    },
  })
}

export const getUploadTask = (taskId: string) =>
  api.get<ApiResponse<UploadTaskStatus>>(`/upload/tasks/${taskId}`)

export const confirmExtraction = (taskId: string, payload: { companyCode: string; companyName?: string; industry?: string; reportYear: number; data: Record<string, ExtractedIndicator> }) =>
  api.post<ApiResponse<void>>(`/upload/tasks/${taskId}/confirm`, payload)

export const getCrawlProgress = (companyCode: string) =>
  api.get<ApiResponse<string>>(`/upload/crawl-progress/${companyCode}`)

export default api
