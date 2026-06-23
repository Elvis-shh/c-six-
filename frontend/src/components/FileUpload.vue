<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUpload } from '@/composables/useUpload'
import { confirmExtraction, getUploadTask, getCrawlProgress } from '@/api'
import type { ExtractedIndicator } from '@/types'

const props = defineProps<{ companyCode?: string; companyName?: string }>()
const emit = defineEmits<{ uploaded: [taskId: string] }>()
const router = useRouter()

const { uploading, progress, error, task, upload } = useUpload()
const dragging = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)
const accepted = '.pdf,.doc,.docx,.xls,.xlsx,.txt,.jpg,.jpeg,.png'
const allowed = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'jpg', 'jpeg', 'png']

const reviewing = ref(false)
const showDetails = ref(false)
const confirmCompany = ref(props.companyCode || '')
const confirmYear = ref(new Date().getFullYear())
const confirming = ref(false)
const crawlState = ref<'idle' | 'saving' | 'crawling' | 'done'>('idle')
const confirmMsg = ref('')
const crawlMsg = ref('')

const labels: Record<string, string> = {
  revenue: '营业收入', profit: '归母净利润', totalAssets: '总资产', totalLiabilities: '总负债',
  cashFlow: '经营现金流', grossMargin: '毛利率', netMargin: '净利率', debtRatio: '资产负债率',
  roe: '净资产收益率', eps: '每股收益', rdExpense: '研发费用', operatingCost: '营业成本',
  sellingExpense: '销售费用', adminExpense: '管理费用', financeExpense: '财务费用',
}

const extractedItems = computed(() => {
  if (!task.value?.extractedData) return []
  return Object.entries(task.value.extractedData).map(([key, val]) => ({
    key,
    label: labels[key] || val.matchedText || key,
    value: val.value,
    unit: val.unit,
    confidence: val.confidence,
  }))
})

const avgConfidence = computed(() => {
  if (!extractedItems.value.length) return 0
  const sum = extractedItems.value.reduce((s, i) => s + i.confidence, 0)
  return Math.round(sum / extractedItems.value.length * 100)
})

async function handleFile(file?: File) {
  if (!file) return
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowed.includes(ext)) { error.value = '不支持的文件类型'; return }
  if (file.size > 50 * 1024 * 1024) { error.value = '文件大小不能超过 50MB'; return }

  const taskId = await upload(file)
  if (!taskId) return
  emit('uploaded', taskId)

  // Poll for extracted data
  for (let i = 0; i < 30; i++) {
    await new Promise(r => setTimeout(r, 1000))
    try {
      const res = await getUploadTask(taskId)
      const d = res.data.data
      if (d?.extractedData) {
        task.value = d
        // Auto-fill company code & year from PDF extraction
        if (d.companyCode) confirmCompany.value = d.companyCode
        if (d.reportYear) confirmYear.value = d.reportYear
        reviewing.value = true
        return
      }
    } catch { /* continue polling */ }
  }
  error.value = '解析超时，请重试'
}

async function confirm() {
  if (!confirmCompany.value.trim()) { confirmMsg.value = '请输入公司代码'; return }
  if (!task.value?.extractedData || crawlState.value !== 'idle') return
  crawlState.value = 'saving'
  confirming.value = true
  confirmMsg.value = ''
  try {
    await confirmExtraction(task.value.taskId, {
      companyCode: confirmCompany.value.trim(),
      companyName: task.value?.companyName || confirmCompany.value.trim(),
      industry: (task.value as any)?.industry || '',
      reportYear: confirmYear.value,
      data: task.value.extractedData as Record<string, ExtractedIndicator>,
    })
    confirming.value = false
    crawlState.value = 'crawling'
    crawlMsg.value = '正在后台爬取近5年年报...'

    const code = confirmCompany.value.trim()
    for (let i = 0; i < 180; i++) {
      await new Promise(r => setTimeout(r, 1500))
      try {
        const res = await getCrawlProgress(code)
        const msg = res.data?.data || ''
        crawlMsg.value = msg || '正在爬取...'
        if (msg.startsWith('done:') || msg.startsWith('failed:')) {
          crawlState.value = 'done'
          return
        }
      } catch { /* continue */ }
    }
    crawlState.value = 'done'
  } catch (e: any) {
    confirmMsg.value = e?.response?.data?.message || '保存失败'
    crawlState.value = 'idle'
  }
}

function cancelReview() {
  reviewing.value = false
  task.value = null
}
</script>

<template>
  <div>
    <!-- Upload zone -->
    <div
      v-if="!reviewing"
      class="upload-zone"
      :class="{ dragging }"
      tabindex="0"
      @click="inputRef?.click()"
      @dragover.prevent="dragging = true"
      @dragleave="dragging = false"
      @drop.prevent="dragging = false; handleFile($event.dataTransfer?.files[0])"
    >
      <input ref="inputRef" hidden type="file" :accept="accepted" @change="handleFile(($event.target as HTMLInputElement).files?.[0])">
      <template v-if="!uploading">
        <strong>上传财报文件</strong>
        <p>拖拽、点击或粘贴文件，支持 PDF / Word / Excel / TXT / 图片</p>
      </template>
      <template v-else>
        <strong>上传中 {{ progress }}%</strong>
        <div class="progress"><span :style="{ width: `${progress}%` }" /></div>
      </template>
      <p v-if="error" class="upload-error">{{ error }}</p>
    </div>

    <!-- Review panel -->
    <div v-else class="review-card">
      <h3>提取结果确认</h3>
      <div class="review-meta">
        <label>公司代码 <input v-model="confirmCompany" placeholder="如 603259" :disabled="!!task?.companyCode" /></label>
        <label>报告年份 <input v-model.number="confirmYear" type="number" min="2000" :max="new Date().getFullYear() + 1" /></label>
        <span v-if="task?.companyName" class="review-hint">{{ task.companyName }}（{{ task.companyCode }}）</span>
      </div>
      <p class="review-summary">
        AI 已从该财报中提取 <strong>{{ extractedItems.length }}</strong> 个指标，
        平均置信度 <strong>{{ avgConfidence }}%</strong>
        <button class="btn-link" @click="showDetails = !showDetails">{{ showDetails ? '收起' : '展开详情' }}</button>
      </p>
      <table v-if="showDetails">
        <thead><tr><th>指标</th><th>值</th><th>单位</th><th>置信度</th></tr></thead>
        <tbody>
          <tr v-for="item in extractedItems" :key="item.key" :class="{ warn: item.confidence < 0.8 }">
            <td>{{ item.label }}</td>
            <td>{{ item.value?.toLocaleString?.('zh-CN') ?? item.value }}</td>
            <td>{{ item.unit }}</td>
            <td>{{ Math.round(item.confidence * 100) }}%</td>
          </tr>
        </tbody>
      </table>
      <div class="review-actions">
        <button class="btn-cancel" @click="cancelReview" :disabled="crawlState === 'crawling'">取消</button>
        <button v-if="crawlState === 'idle' || crawlState === 'saving'" class="btn-confirm" :disabled="crawlState === 'saving'" @click="confirm">{{ crawlState === 'saving' ? '保存中...' : '确认并爬取近5年年报' }}</button>
        <button v-if="crawlState === 'done'" class="btn-goto" @click="router.push(`/dashboard/${confirmCompany.trim()}`)">查看报告</button>
      </div>
      <p v-if="confirmMsg" class="confirm-msg">{{ confirmMsg }}</p>
      <div v-if="crawlState === 'crawling' || crawlState === 'done'" class="crawl-progress">
        <div class="progress"><span :style="{ width: crawlState === 'done' ? '100%' : '60%' }" /></div>
        <p>{{ crawlState === 'done' ? '爬取完成，可查看报告' : crawlMsg }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-zone {
  padding: 22px;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.upload-zone.dragging { border-color: var(--primary); background: var(--primary-light); }
.upload-zone p { margin-top: 6px; color: var(--text-secondary); font-size: 13px; }
.progress { height: 8px; margin-top: 12px; overflow: hidden; border-radius: 999px; background: var(--border-light); }
.progress span { display: block; height: 100%; background: var(--primary); transition: width 0.4s ease; }
.upload-error { color: #ef4444 !important; }

.review-card {
  margin-top: 16px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
}
.review-card h3 { margin: 0 0 14px; font-size: 16px; }
.review-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.review-meta label { font-size: 13px; color: var(--text-secondary); }
.review-meta input {
  margin-left: 6px;
  padding: 4px 8px;
  border: 1px solid var(--border);
  border-radius: 4px;
  width: 100px;
  font-size: 13px;
}
.review-hint { font-size: 12px; color: var(--primary); }

table { width: 100%; margin: 12px 0; border-collapse: collapse; font-size: 13px; }
th, td { padding: 8px 10px; border-bottom: 1px solid var(--border-light); text-align: left; }
th { color: var(--text-muted); font-weight: 600; }
.warn { background: #fffbeb; }

.review-actions { display: flex; gap: 10px; margin-top: 14px; }
.btn-cancel {
  padding: 8px 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text-secondary);
  cursor: pointer;
}
.btn-confirm {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-goto {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: #10b981;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}
.confirm-msg { margin-top: 8px; font-size: 13px; color: var(--primary); }

.crawl-progress {
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--primary-light, #eff6ff);
  border-radius: 8px;
}
.crawl-progress .progress {
  margin-top: 0;
  margin-bottom: 6px;
}
.crawl-progress p {
  margin: 0;
  font-size: 12px;
  color: var(--primary);
}

.review-summary {
  margin: 8px 0 4px;
  font-size: 13px;
  color: var(--text-secondary);
}
.review-summary strong { color: var(--text); }
.btn-link {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  font-size: 13px;
  padding: 0;
  margin-left: 8px;
  text-decoration: underline;
}
</style>
