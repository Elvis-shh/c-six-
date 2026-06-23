// types/index.ts — SmartReport TypeScript 类型定义

export interface Company {
  code: string
  name: string
  shortName: string
  industry: string
  market: 'SH' | 'SZ' | 'BJ' | 'HK'
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface KpiItem {
  key: string
  name: string
  value: number
  unit: string
  yoy: number | null
  trend: 'up' | 'down' | 'down_good'
  explanation?: string
}

export interface KpiResponse {
  company: Company
  reportYear: number
  reportType: string
  kpis: KpiItem[]
}

export interface MetricSeries {
  key: string
  name: string
  unit: string
  values: (number | null)[]
}

export interface TimelineResponse {
  years: string[]
  metrics: MetricSeries[]
}

export interface IndicatorBenchmark {
  key: string
  name: string
  companyValue: number
  industryAvg: number
  industryMedian: number
  rank: string
  rankLabel: string
  unit: string
}

export interface BenchmarkResponse {
  industry: string
  indicators: IndicatorBenchmark[]
}

export interface AnalysisHistoryItem {
  code: string
  name: string
  timestamp: number
}

export interface HighlightItem {
  id: number
  icon: string
  title: string
  description: string
  ruleKey: string
}

export interface RiskItem {
  id: number
  icon: string
  title: string
  description: string
  ruleKey: string
}

export interface PredictSeries {
  key: string
  name: string
  type: 'solid' | 'dashed'
  values: (number | null)[]
}

export interface PredictResponse {
  years: string[]
  series: PredictSeries[]
  insights: Record<string, {
    name: string
    unit: string
    lastValue: number
    predictedValue: number
    change: number
    trend: string
    r2: number
    slope: number
  }>
  /** Story 5.2: 富文本预测洞察（4 段文案） */
  fullInsights?: PredictInsightResponse
}

/** 预测洞察面板数据 */
export interface PredictInsightResponse {
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

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  refs?: { source: string; snippet?: string; content?: string; score: number; page?: number }[]
  followUps?: string[]
}

/** Phase 4: 用户认证 */
export interface AuthResponse {
  userId: number
  email: string
  nickname: string
  accessToken: string
  refreshToken: string
}

export interface RegisterRequest {
  email: string
  password: string
  nickname?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface HistoryItem {
  id: number
  code: string
  name: string
  timestamp: number
}

export interface UploadTaskResponse {
  taskId: string
  status: string
  fileName: string
  message: string
}

export interface ExtractedIndicator {
  value: number | null
  unit: string
  confidence: number
  method: string
  matchedText?: string
}

export interface UploadTaskStatus {
  taskId: string
  status: string
  stage: string
  message: string
  percent: number
  extractedData?: Record<string, ExtractedIndicator>
  companyCode?: string
  companyName?: string
  reportYear?: number
  industry?: string
}

export interface IndicatorDetail {
  key: string
  name: string
  unit: string
  value: number
  yoy: number | null
  trend: string
  category: string
  explanation: string
  evaluation: string
  sortOrder: number
}

export interface IndicatorDetailResponse {
  indicators: IndicatorDetail[]
}
