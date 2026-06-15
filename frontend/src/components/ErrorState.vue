<script setup lang="ts">
defineProps<{ message?: string }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="error-state">
    <div class="error-icon">⚠️</div>
    <p>{{ message || '加载失败，请稍后重试' }}</p>
    <button class="retry-btn" @click="$emit('retry')">重试</button>
  </div>
</template>

<style scoped>
.error-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-secondary);
}
.error-icon { font-size: 36px; margin-bottom: 12px; }
.retry-btn {
  margin-top: 16px;
  padding: 8px 24px;
  background: var(--primary);
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
}
.retry-btn:hover { background: var(--primary-dark); }
</style>
