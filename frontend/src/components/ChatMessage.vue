<script setup lang="ts">
import { marked } from 'marked'
import { computed } from 'vue'
import type { ChatMessage } from '@/types'

const props = defineProps<{ message: ChatMessage }>()

const html = computed(() => marked.parse(props.message.content || '...') as string)
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="bubble">
      <div class="content markdown-body" v-html="html" />
      <div v-if="message.refs?.length" class="refs">
        <span v-for="ref in message.refs" :key="ref.source">{{ ref.source }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: 14px;
}
.chat-message.user { justify-content: flex-end; }
.bubble {
  max-width: 82%;
  padding: 11px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
}
.content :deep(p) { margin: 0 0 8px; }
.content :deep(p:last-child) { margin-bottom: 0; }
.content :deep(strong) { font-weight: 700; }
.content :deep(ul),
.content :deep(ol) { margin: 6px 0 6px 18px; }
.content :deep(code) {
  padding: 1px 4px;
  border-radius: 4px;
  background: rgba(15, 23, 42, 0.08);
  font-family: Consolas, monospace;
}
.user .bubble {
  color: #fff;
  background: var(--primary);
  border-bottom-right-radius: 4px;
}
.assistant .bubble {
  background: var(--surface-alt);
  border: 1px solid var(--border-light);
  border-bottom-left-radius: 4px;
}
.refs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.refs span {
  padding: 2px 6px;
  border-radius: 999px;
  background: var(--primary-light);
  color: var(--primary-dark);
  font-size: 11px;
}
</style>
