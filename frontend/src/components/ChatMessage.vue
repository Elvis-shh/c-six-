<script setup lang="ts">
import type { ChatMessage } from '@/types'

defineProps<{ message: ChatMessage }>()
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="bubble">
      <p>{{ message.content || '...' }}</p>
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
  white-space: pre-wrap;
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
