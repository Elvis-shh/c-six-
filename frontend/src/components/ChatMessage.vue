<script setup lang="ts">
import { marked } from 'marked'
import { computed } from 'vue'
import type { ChatMessage } from '@/types'

const props = defineProps<{ message: ChatMessage }>()
const emit = defineEmits<{ followup: [question: string] }>()

const html = computed(() => marked.parse(props.message.content || '...') as string)

function normalizeSource(source: string) {
  const fileName = source.split(/[\\/]/).pop() || source
  return fileName
    .replace(/_em_/g, '')
    .replace(/\.pdf/gi, '')
    .replace(/第\s*\d+\s*页/gi, '')
    .replace(/_+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function sourceKey(source: string) {
  return normalizeSource(source)
    .toLowerCase()
    .replace(/[：:]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

const groupedRefs = computed(() => {
  const groups = new Map<string, { source: string; pages: number[]; snippets: string[] }>()
  for (const ref of props.message.refs || []) {
    const source = normalizeSource(ref.source || '财报原文')
    const key = sourceKey(ref.source || '财报原文')
    const existing = groups.get(key) || { source, pages: [], snippets: [] }
    if (typeof ref.page === 'number' && !existing.pages.includes(ref.page)) {
      existing.pages.push(ref.page)
    }
    const snippet = (ref.snippet || ref.content || '').trim()
    if (snippet && !existing.snippets.includes(snippet)) {
      existing.snippets.push(snippet)
    }
    groups.set(key, existing)
  }
  return [...groups.values()].map(item => ({
    ...item,
    pages: [...item.pages].sort((a, b) => a - b),
  }))
})
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="bubble">
      <div class="content markdown-body" v-html="html" />
      <div v-if="groupedRefs.length" class="refs">
        <p class="refs-title">引用来源</p>
        <details v-for="ref in groupedRefs" :key="ref.source" class="ref-item">
          <summary>
            {{ ref.source }}
            <span v-if="ref.pages.length"> · 第{{ ref.pages.join('、') }}页</span>
          </summary>
          <p v-for="snippet in ref.snippets" :key="snippet">{{ snippet }}</p>
        </details>
      </div>
      <div v-if="message.followUps?.length" class="followups">
        <p class="followups-title">你还可以继续问</p>
        <button v-for="item in message.followUps" :key="item" type="button" @click="emit('followup', item)">{{ item }}</button>
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
  display: grid;
  gap: 6px;
  margin-top: 12px;
  opacity: 0.88;
}
.refs-title {
  margin: 0 0 2px;
  color: var(--text-muted);
  font-size: 11px;
}
.ref-item {
  padding: 6px 8px;
  border-radius: 8px;
  border: 1px solid var(--border-light);
  background: rgba(15, 23, 42, 0.06);
  color: var(--text-secondary);
  font-size: 12px;
}
.ref-item summary {
  cursor: pointer;
  color: var(--text-muted);
}
.ref-item p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  line-height: 1.5;
}
.followups {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}
.followups-title {
  margin: 0;
  color: var(--text-muted);
  font-size: 11px;
}
.followups button {
  padding: 8px 10px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: rgba(37, 99, 235, 0.06);
  color: var(--primary-dark);
  text-align: left;
}
</style>
