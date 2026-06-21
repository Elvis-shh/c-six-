// api/index.ts — SmartReport API 请求层
import axios from 'axios'
import type { ApiResponse, Company, KpiResponse, TimelineResponse, BenchmarkResponse, HighlightItem, RiskItem, PredictResponse, PredictInsightResponse, UploadTaskResponse, UploadTaskStatus, ExtractedIndicator, AuthResponse, RegisterRequest, LoginRequest, HistoryItem } from '@/types'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Phase 4: JWT 拦截器 — 自动附加 Authorization header
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Phase 4: 401 响应拦截 — 自动尝试 refreshToken 续期
let isRefreshing = false
api.interceptors.response.use(
  res => res,
  async error => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      if (!isRefreshing) {
        isRefreshing = true
        try {
          const refresh = localStorage.getItem('refreshToken')
          if (refresh) {
            const { data } = await axios.post('/api/v1/auth/refresh', {}, {
              headers: { Authorization: `Bearer ${refresh}` }
            })
            if (data.code === 0 && data.data) {
              localStorage.setItem('accessToken', data.data.accessToken)
              localStorage.setItem('refreshToken', data.data.refreshToken)
              original.headers.Authorization = `Bearer ${data.data.accessToken}`
              return api(original)
            }
          }
        } catch (e) { /* refresh failed */ }
        finally { isRefreshing = false }
      }
    }
    return Promise.reject(error)
  }
)

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

export const getPredictInsights = (companyCode: string) =>
  api.get<ApiResponse<PredictInsightResponse>>(`/analysis/${companyCode}/predict/insights`)

// ============================================================
// Phase 2: 后端导出服务
// ============================================================
export const submitBackendExport = (companyCode: string, format: 'pdf' | 'xlsx' | 'doc') =>
  api.post<ApiResponse<{ taskId: string; status: string }>>('/export', { companyCode, format })

export const getExportTask = (taskId: string) =>
  api.get<ApiResponse<{ taskId: string; status: string; progress: number; fileName: string }>>(`/export/tasks/${taskId}`)

export const downloadExportFile = (taskId: string) =>
  api.get(`/export/download/${taskId}`, { responseType: 'blob' })

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
    headers: { 'Content-Type': 'application/json', 'Accept': 'text/event-stream' },
    body: JSON.stringify(payload),
  })

export const uploadReport = (file: File, onUploadProgress?: (percent: number) => void) => {
  const formData = new FormData()
  formData.append('file', file)
  return api.post<ApiResponse<UploadTaskResponse>>('/upload/report', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: event => {
      if (event.total && onUploadProgress) onUploadProgress(Math.round((event.loaded / event.total) * 100))
    },
  })
}

export const getUploadTask = (taskId: string) =>
  api.get<ApiResponse<UploadTaskStatus>>(`/upload/tasks/${taskId}`)

export const confirmExtraction = (taskId: string, payload: { companyCode: string; reportYear: number; data: Record<string, ExtractedIndicator> }) =>
  api.post<ApiResponse<void>>(`/upload/tasks/${taskId}/confirm`, payload)

// ============================================================
// Phase 4: 用户认证
// ============================================================
export const registerUser = (payload: RegisterRequest) =>
  api.post<ApiResponse<AuthResponse>>('/auth/register', payload)

export const loginUser = (payload: LoginRequest) =>
  api.post<ApiResponse<AuthResponse>>('/auth/login', payload)

export const refreshToken = (refreshToken: string) =>
  axios.post('/api/v1/auth/refresh', {}, {
    headers: { Authorization: `Bearer ${refreshToken}` }
  })

export const logoutUser = () =>
  api.post<ApiResponse<void>>('/auth/logout')

export const getMe = () =>
  api.get<ApiResponse<AuthResponse>>('/auth/me')

// ============================================================
// Phase 4: 历史同步
// ============================================================
export const getHistory = () =>
  api.get<ApiResponse<HistoryItem[]>>('/history')

export const addHistoryItem = (companyCode: string, companyName: string) =>
  api.post<ApiResponse<void>>('/history', { companyCode, companyName })

export const deleteHistoryItem = (id: number) =>
  api.delete<ApiResponse<void>>(`/history/${id}`)

export const clearHistory = () =>
  api.delete<ApiResponse<void>>('/history')

export const syncHistory = (localItems: { code: string; name: string; timestamp: number }[]) =>
  api.post<ApiResponse<HistoryItem[]>>('/history/sync', localItems)

export default api
