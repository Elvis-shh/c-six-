import { ref } from 'vue'
import { getUploadTask, uploadReport } from '@/api'
import type { UploadTaskStatus } from '@/types'

export function useUpload() {
  const uploading = ref(false)
  const progress = ref(0)
  const error = ref('')
  const task = ref<UploadTaskStatus | null>(null)

  async function upload(file: File) {
    error.value = ''
    uploading.value = true
    progress.value = 0
    try {
      const res = await uploadReport(file, percent => { progress.value = percent })
      const taskId = res.data.data.taskId
      const status = await getUploadTask(taskId)
      task.value = status.data.data
      return taskId
    } catch (e: any) {
      error.value = e?.response?.data?.message || '上传失败，请重试'
      return null
    } finally {
      uploading.value = false
    }
  }

  return { uploading, progress, error, task, upload }
}
