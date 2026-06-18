<script setup lang="ts">
import { useRouter } from 'vue-router'
import SearchBox from '@/components/SearchBox.vue'
import FileUpload from '@/components/FileUpload.vue'
import { getHotCompanies } from '@/api'
import type { Company } from '@/types'
import { ref, onMounted } from 'vue'

const router = useRouter()
const hotList = ref<Company[]>([])
const uploadTaskId = ref('')

onMounted(async () => {
  const res = await getHotCompanies()
  hotList.value = res.data.data
})

function selectChip(company: Company) {
  router.push(`/dashboard/${company.code}`)
}
</script>

<template>
  <div class="search-page">
    <div class="search-hero">
      <h1 class="hero-title">📊 SmartReport</h1>
      <p class="hero-sub">智能财报研读助手 — 让财务数据一目了然</p>
      <SearchBox placeholder="搜索公司名称或股票代码，如「贵州茅台」" />
      <div class="upload-entry">
        <FileUpload @uploaded="uploadTaskId = $event" />
        <p v-if="uploadTaskId" class="upload-task">解析任务已创建：{{ uploadTaskId }}</p>
      </div>
    </div>

    <div class="hot-section">
      <h3>🔥 热门分析</h3>
      <div class="chip-list">
        <button
          v-for="c in hotList" :key="c.code"
          class="chip"
          @click="selectChip(c)"
        >
          {{ c.name }}
          <span class="chip-code">{{ c.code }}</span>
        </button>
      </div>
    </div>

    <footer class="search-footer">
      <p>支持 A 股上市公司财报数据 | 数据仅供参考，不构成投资建议</p>
    </footer>
  </div>
</template>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 100vh;
  padding: 80px 20px 40px;
}
.search-hero {
  text-align: center;
  margin-bottom: 48px;
  width: 100%;
  max-width: 540px;
}
.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: var(--text);
  margin-bottom: 8px;
}
.hero-sub {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}
.upload-entry {
  margin-top: 22px;
}
.upload-task {
  margin-top: 10px;
  color: var(--primary-dark);
  font-size: 13px;
}

.hot-section {
  width: 100%;
  max-width: 600px;
  text-align: center;
}
.hot-section h3 {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 16px;
}
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}
.chip {
  padding: 8px 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  font-size: 14px;
  color: var(--text);
  cursor: pointer;
  transition: all 0.2s;
}
.chip:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}
.chip-code {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: 4px;
}

.search-footer {
  margin-top: auto;
  padding-top: 40px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}
</style>
