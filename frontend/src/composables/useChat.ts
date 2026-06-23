import { nextTick, type Ref } from 'vue'
import { sendChatMessage } from '@/api'
import { useChatStore } from '@/stores'
import type { ChatMessage } from '@/types'

function stripTrailingRefs(text: string) {
  return text
    .replace(/\n*\[FOLLOWUPS\][\s\S]*$/i, '')
    .replace(/\n*参考(?:资料|来源|文献|数据)?[：:][\s\S]*$/i, '')
    .replace(/\n*引用(?:来源|文献)?[：:][\s\S]*$/i, '')
    .replace(/\n*\[来源[：:][\s\S]*$/i, '')
    .replace(/\n*(?:数据|资料)来源[：:][\s\S]*$/i, '')
    .replace(/\n*以上(?:内容|数据|分析)?(?:来源于|参考|基于|引自).*$/i, '')
    .replace(/\n*（(?:数据|资料|参考)来源[：:][^）]*）\s*$/i, '')
    .replace(/\n*本回答(?:数据)?(?:来源于|参考|基于).*$/i, '')
    .replace(/\n*详见[：:].*$/i, '')
    .replace(/\n*查阅[：:].*$/i, '')
    .replace(/\n*[-\s]*参考[：:][^\n]*$/, '')
    .trim()
}

export function useChat(companyCode: Ref<string>) {
  const store = useChatStore()
  const defaultFollowUps = [
    '这家公司最大的风险是什么？',
    '现金流和利润匹配吗？',
    '和同行比它处于什么水平？'
  ]

  const followUpPool: Record<string, string[]> = {
    '现金流': ['利润含金量高吗？', '应收账款有没有压力？', '明年现金流会继续改善吗？', '经营现金流为什么波动这么大？', '自由现金流情况怎么样？'],
    '风险': ['最大的单一风险是什么？', '负债压力和同行比高吗？', '有哪些指标已经开始转弱？', '这个风险短期内能化解吗？', '管理层有什么应对措施？'],
    '盈利': ['毛利率为什么变化？', '净利润增长可持续吗？', '和同行比赚钱吗？', '费用控制怎么样？', '利润质量高吗？'],
    '营收': ['收入增长主要靠什么驱动？', '各业务板块表现如何？', '海外收入占比多少？', '明年收入还能保持这个增速吗？'],
    '负债': ['短期偿债压力大吗？', '负债结构合理吗？', '利息支出负担重吗？', '融资成本有没有上升？'],
    '研发': ['研发投入主要在哪些方向？', '研发投入和同行比什么水平？', '研发成果转化怎么样？'],
    '资产': ['资产质量怎么样？', '有没有大额减值风险？', '资产周转效率如何？'],
  }

  const allFollowUps = [
    '这家公司最大的风险是什么？',
    '现金流和利润匹配吗？',
    '和同行比它处于什么水平？',
    '未来成长空间在哪里？',
    '核心竞争力是什么？',
    '分红情况怎么样？',
    '管理层靠谱吗？',
    '估值贵不贵？',
  ]

  async function sendMessage(text: string) {
    const message = text.trim()
    if (!message || store.isLoading) return

    store.addMessage('user', message)
    store.ensureAssistantMessage()
    store.isLoading = true
    store.progress = 10

    // Auto-advance progress even without tokens
    const progressTimer = setInterval(() => {
      if (store.progress < 85) store.progress += 1
    }, 200)

    try {
      const response = await sendChatMessage({ companyCode: companyCode.value, message, sessionId: store.sessionId })
      if (!response.ok || !response.body) {
        throw new Error(`chat request failed: ${response.status}`)
      }
      const reader = response.body?.getReader()
      if (!reader) throw new Error('浏览器不支持流式响应')

      const decoder = new TextDecoder()
      let buffer = ''
      let content = ''
      let receivedToken = false
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const payload = JSON.parse(line.replace(/^data:\s*/, ''))
          if (payload.type === 'thinking') {
            store.progress = Math.max(store.progress, 30)
          }
          if (payload.type === 'token') {
            receivedToken = true
            content += payload.content
            store.updateLastAssistant(content)
            await nextTick()
          }
          if (payload.type === 'followups') {
            store.updateLastAssistant(content, undefined, payload.followUps as string[])
          }
          if (payload.type === 'refs') {
            store.progress = 95
            content = stripTrailingRefs(content)
            store.updateLastAssistant(content, payload.refs as ChatMessage['refs'], buildFollowUps(message))
          }
          if (payload.type === 'done') {
            store.progress = 100
          }
        }
      }
      if (!receivedToken) {
        throw new Error('no chat tokens received')
      }
    } catch (error) {
      store.updateLastAssistant('智能问答暂时不可用，请稍后重试。')
    } finally {
      clearInterval(progressTimer)
      store.progress = 100
      store.isLoading = false
      setTimeout(() => { store.progress = 0 }, 600)
    }
  }

  function buildFollowUps(message: string) {
    const matched: string[] = []
    for (const [keyword, questions] of Object.entries(followUpPool)) {
      if (message.includes(keyword)) {
        matched.push(...questions)
      }
    }
    if (matched.length >= 3) {
      return shuffle(matched).slice(0, 3)
    }
    // Mix matched with some general ones
    const pool = [...matched, ...allFollowUps]
    return [...new Set(shuffle(pool))].slice(0, 3)
  }

  function shuffle<T>(arr: T[]): T[] {
    const a = [...arr]
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]]
    }
    return a
  }

  return { store, sendMessage }
}
