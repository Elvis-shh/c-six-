<script setup lang="ts">
import { ref } from 'vue'
import { useUpload } from '@/composables/useUpload'

const emit = defineEmits<{ uploaded: [taskId: string] }>()
const { uploading, progress, error, upload } = useUpload()
const dragging = ref(false)
const inputRef = ref<HTMLInputElement | null>(null)
const accepted = '.pdf,.doc,.docx,.xls,.xlsx,.txt,.jpg,.jpeg,.png'
const allowed = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt', 'jpg', 'jpeg', 'png']

async function handleFile(file?: File) {
  if (!file) return
  const ext = file.name.split('.').pop()?.toLowerCase() || ''
  if (!allowed.includes(ext)) {
    error.value = '不支持的文件类型'
    return
  }
  if (file.size > 50 * 1024 * 1024) {
    error.value = '文件大小不能超过 50MB'
    return
  }
  const taskId = await upload(file)
  if (taskId) emit('uploaded', taskId)
}
</script>

<template>
  <div
    class="upload-zone"
    :class="{ dragging }"
    tabindex="0"
    @click="inputRef?.click()"
    @dragover.prevent="dragging = true"
    @dragleave="dragging = false"
    @drop.prevent="dragging = false; handleFile($event.dataTransfer?.files[0])"
    @paste="handleFile($event.clipboardData?.files[0])"
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
.upload-zone.dragging {
  border-color: var(--primary);
  background: var(--primary-light);
}
.upload-zone p {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}
.progress {
  height: 8px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--border-light);
}
.progress span {
  display: block;
  height: 100%;
  background: var(--primary);
}
.upload-error { color: var(--risk-red) !important; }
</style>
