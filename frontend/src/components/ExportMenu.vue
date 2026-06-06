<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ disabled?: boolean }>()

const emit = defineEmits<{
  (e: 'export', format: 'png' | 'pdf' | 'doc' | 'xlsx'): void
}>()

const showMenu = ref(false)

function select(format: 'png' | 'pdf' | 'doc' | 'xlsx') {
  showMenu.value = false
  emit('export', format)
}
</script>

<template>
  <div class="export-wrapper">
    <button class="export-btn" :disabled="disabled" @click="showMenu = !showMenu" title="导出报告">
      {{ disabled ? '⏳ 导出中...' : '📥 导出' }}
    </button>
    <div v-if="showMenu" class="export-dropdown" @mouseleave="showMenu = false">
      <button class="export-item" @click="select('png')">
        <span class="export-icon">🖼️</span> PNG 图片
      </button>
      <button class="export-item" @click="select('pdf')">
        <span class="export-icon">📄</span> PDF 文档
      </button>
      <button class="export-item" @click="select('doc')">
        <span class="export-icon">📝</span> Word (.doc)
      </button>
      <button class="export-item" @click="select('xlsx')">
        <span class="export-icon">📊</span> Excel (.xlsx)
      </button>
    </div>
  </div>
</template>

<style scoped>
.export-wrapper {
  position: relative;
}

.export-btn {
  padding: 8px 16px;
  border: 1px solid var(--border, #e8ecf1);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.export-btn:hover {
  background: var(--primary, #3b82f6);
  color: #fff;
  border-color: var(--primary, #3b82f6);
}

.export-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 180px;
  background: var(--surface);
  border: 1px solid var(--border, #e8ecf1);
  border-radius: 10px;
  box-shadow: 0 8px 30px rgba(0,0,0,.1);
  padding: 6px;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.export-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;
}

.export-item:hover {
  background: var(--bg-muted, #f0f4f8);
}

.export-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
}
</style>
