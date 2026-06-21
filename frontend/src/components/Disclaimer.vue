<script setup lang="ts">
defineProps<{
  /** 自定义免责声明文本，不传则使用默认文本 */
  text?: string
}>()
</script>

<template>
  <div class="disclaimer-box">
    <div class="disclaimer-icon">⚠️</div>
    <div class="disclaimer-content">
      <p class="disclaimer-title">免责声明</p>
      <p class="disclaimer-text">
        {{ text || '本报告由 SmartReport 自动生成，数据基于公开财报信息。所有分析和预测仅供参考学习，不构成任何投资建议。投资有风险，入市需谨慎。' }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.disclaimer-box {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border: 1px dashed #f59e0b;
  border-radius: 10px;
  background: #fffbeb;
  margin-top: 20px;
}

.disclaimer-icon {
  font-size: 18px;
  flex-shrink: 0;
  line-height: 1.5;
}

.disclaimer-content {
  flex: 1;
}

.disclaimer-title {
  font-size: 13px;
  font-weight: 700;
  color: #92400e;
  margin: 0 0 4px 0;
}

.disclaimer-text {
  font-size: 12px;
  color: #a16207;
  margin: 0;
  line-height: 1.6;
}
</style>
