import { nextTick, type Ref } from 'vue'
import { sendChatMessage } from '@/api'
import { useChatStore } from '@/stores'
import type { ChatMessage } from '@/types'

export function useChat(companyCode: Ref<string>) {
  const store = useChatStore()
  const defaultFollowUps = [
    '这家公司最大的风险是什么？',
    '现金流和利润匹配吗？',
    '和同行比它处于什么水平？'
  ]

  async function sendMessage(text: string) {
    const message = text.trim()
    if (!message || store.isLoading) return

    store.addMessage('user', message)
    store.ensureAssistantMessage()
    store.isLoading = true
    store.progress = 24

    try {
      const response = await sendChatMessage({ companyCode: companyCode.value, message, sessionId: store.sessionId })
      const reader = response.body?.getReader()
      if (!reader) throw new Error('浏览器不支持流式响应')

      const decoder = new TextDecoder()
      let buffer = ''
      let content = ''
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
            store.progress = Math.max(store.progress, 52)
          }
          if (payload.type === 'token') {
            store.progress = Math.min(94, store.progress + 2)
            content += payload.content
            store.updateLastAssistant(content)
            await nextTick()
          }
          if (payload.type === 'refs') {
            store.progress = Math.max(store.progress, 96)
            store.updateLastAssistant(content, payload.refs as ChatMessage['refs'], buildFollowUps(message))
          }
          if (payload.type === 'done') {
            store.progress = 100
          }
        }
      }
    } catch (error) {
      store.updateLastAssistant('智能问答暂时不可用，请稍后重试。')
    } finally {
      store.isLoading = false
      setTimeout(() => { store.progress = 0 }, 400)
    }
  }

  function buildFollowUps(message: string) {
    if (message.includes('现金流')) {
      return ['利润含金量高吗？', '应收账款有没有压力？', '明年现金流会继续改善吗？']
    }
    if (message.includes('风险')) {
      return ['最大的单一风险是什么？', '负债压力和同行比高吗？', '有哪些指标已经开始转弱？']
    }
    if (message.includes('盈利') || message.includes('赚钱')) {
      return ['毛利率为什么变化？', '净利润增长可持续吗？', '和同行比赚钱吗？']
    }
    return defaultFollowUps
  }

  return { store, sendMessage }
}
