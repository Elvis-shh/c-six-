<script setup lang="ts">
import { computed, nextTick, ref, watch, type Ref } from 'vue'
import { useChat } from '@/composables/useChat'
import ChatMessage from '@/components/ChatMessage.vue'

const props = defineProps<{ companyCode: string; companyName?: string }>()
const companyCodeRef = computed(() => props.companyCode) as Ref<string>
const { store, sendMessage } = useChat(companyCodeRef)
const input = ref('')
const messagesEl = ref<HTMLElement | null>(null)
const suggestions = [
  '这家公司像一家店，赚钱能力怎么样？',
  '它真正收回来的钱够不够？',
  '和同行比，最大的风险是什么？',
  '这份年报里最值得看的三件事是什么？'
]

watch(() => store.messages.length, async () => {
  await nextTick()
  messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight, behavior: 'smooth' })
})

async function submit(text = input.value) {
  input.value = ''
  await sendMessage(text)
}
</script>

<template>
  <Transition name="slide">
    <aside v-if="store.isOpen" class="chat-panel">
      <header class="chat-header">
        <div>
          <strong>AI 财报助手</strong>
          <span>{{ companyName || companyCode }}</span>
        </div>
        <button aria-label="关闭" @click="store.close">x</button>
      </header>

      <div ref="messagesEl" class="chat-messages">
        <div v-if="store.messages.length === 0" class="welcome">
          <h3>可以直接问我财报问题</h3>
          <p>我会先查财报原文和行业常识，再用尽量好懂的话解释，并给出来源。</p>
          <button v-for="item in suggestions" :key="item" @click="submit(item)">{{ item }}</button>
        </div>
        <div v-if="store.isLoading" class="progress-card">
          <div class="progress-text">AI 正在整理财报内容 {{ store.progress }}%</div>
          <div class="progress-track"><div class="progress-bar" :style="{ width: `${store.progress}%` }" /></div>
        </div>
        <ChatMessage v-for="msg in store.messages" :key="msg.timestamp" :message="msg" @followup="submit" />
      </div>

      <div class="chat-input">
        <textarea
          v-model="input"
          rows="2"
          placeholder="输入你的问题..."
          @keydown.enter.exact.prevent="submit()"
        />
        <button :disabled="!input.trim() || store.isLoading" @click="submit()">发送</button>
      </div>
      <p class="disclaimer">仅供学习和辅助分析，不构成投资建议。</p>
    </aside>
  </Transition>
</template>

<style scoped>
.chat-panel {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  width: 400px;
  height: 100vh;
  background: var(--surface);
  border-left: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
}
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px;
  border-bottom: 1px solid var(--border-light);
}
.chat-header span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
}
.chat-header button {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--surface-alt);
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
}
.progress-card {
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(37, 99, 235, 0.05), rgba(37, 99, 235, 0.02));
}
.progress-text {
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 12px;
}
.progress-track {
  overflow: hidden;
  height: 7px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
}
.progress-bar {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--primary), #7c3aed);
  transition: width 0.25s ease;
}
.welcome {
  padding: 16px;
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  background: var(--surface-alt);
}
.welcome p {
  margin: 8px 0 14px;
  color: var(--text-secondary);
  font-size: 13px;
}
.welcome button {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  color: var(--primary-dark);
  background: var(--primary-light);
  text-align: left;
}
.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid var(--border-light);
}
.chat-input textarea { resize: none; }
.chat-input button {
  align-self: end;
  padding: 10px 16px;
  border-radius: 8px;
  color: #fff;
  background: var(--primary);
}
.chat-input button:disabled { opacity: 0.5; }
.disclaimer {
  margin: -4px 16px 12px;
  color: var(--text-muted);
  font-size: 11px;
  opacity: 0.72;
}
.slide-enter-active, .slide-leave-active { transition: transform 0.25s ease; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }
@media (max-width: 520px) {
  .chat-panel { width: 100vw; }
}
</style>
