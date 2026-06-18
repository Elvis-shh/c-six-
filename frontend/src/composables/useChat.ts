import { nextTick, type Ref } from 'vue'
import { sendChatMessage } from '@/api'
import { useChatStore } from '@/stores'
import type { ChatMessage } from '@/types'

export function useChat(companyCode: Ref<string>) {
  const store = useChatStore()

  async function sendMessage(text: string) {
    const message = text.trim()
    if (!message || store.isLoading) return

    store.addMessage('user', message)
    store.updateLastAssistant('')
    store.isLoading = true

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
          if (payload.type === 'token') {
            content += payload.content
            store.updateLastAssistant(content)
            await nextTick()
          }
          if (payload.type === 'refs') {
            store.updateLastAssistant(content, payload.refs as ChatMessage['refs'])
          }
        }
      }
    } catch (error) {
      store.updateLastAssistant('智能问答暂时不可用，请稍后重试。')
    } finally {
      store.isLoading = false
    }
  }

  return { store, sendMessage }
}
