<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useHistory, relativeTime } from '@/composables/useHistory'
import { useExport } from '@/composables/useExport'
import { useDashboardStore } from '@/stores'
import { useAuthStore } from '@/stores/authStore'
import SearchBox from '@/components/SearchBox.vue'
import ExportMenu from '@/components/ExportMenu.vue'
import ReportLibraryModal from '@/components/ReportLibraryModal.vue'
import { logoutUser } from '@/api'

const router = useRouter()
const route = useRoute()
const { items, remove, clear } = useHistory()
const showDropdown = ref(false)
const showLibrary = ref(false)
const dashboardStore = useDashboardStore()
const auth = useAuthStore()

const showExport = computed(() => route.name === 'Dashboard')
const showNavSearch = computed(() => route.name !== 'Search')
const companyName = computed(() => dashboardStore.kpiData?.company?.name || '未知公司')

const { exporting, format, exportPNG, exportPDF, exportWord, exportExcel } = useExport(companyName)

function goHome() { router.push('/search') }

function selectFromHistory(code: string) {
  showDropdown.value = false
  router.push(`/dashboard/${code}`)
}

async function handleExport(fmt: 'png' | 'pdf' | 'doc' | 'xlsx') {
  switch (fmt) {
    case 'png': await exportPNG(); break
    case 'pdf': await exportPDF(); break
    case 'doc': await exportWord(); break
    case 'xlsx': await exportExcel(dashboardStore); break
  }
}

async function handleLogout() {
  try { await logoutUser() } catch { /* ignore */ }
  auth.logout()
}
</script>

<template>
  <nav class="navbar">
    <div class="navbar-inner">
      <div class="navbar-left">
        <span class="brand" @click="goHome">📊 SmartReport</span>
        <button class="library-btn" @click="showLibrary = true">财报库</button>
      </div>

      <div v-if="showNavSearch" class="navbar-center">
        <SearchBox compact placeholder="搜索公司名称或代码..." />
      </div>
      <div v-else class="navbar-center navbar-center-empty" />

      <div class="navbar-right">
        <ExportMenu v-if="showExport" :disabled="exporting" @export="handleExport" />
        <div class="history-wrapper">
          <button class="history-btn" @click="showDropdown = !showDropdown">
            📋 最近分析
          </button>
          <div v-if="showDropdown" class="history-dropdown" @mouseleave="showDropdown = false">
            <div v-if="items.length === 0" class="history-empty">
              暂无分析记录
            </div>
            <div
              v-for="item in items"
              :key="item.code"
              class="history-item"
              @click="selectFromHistory(item.code)"
            >
              <div class="history-item-main">
                <span class="history-name">{{ item.name }}</span>
                <span class="history-code">
                  {{ item.code }}
                  <template v-if="item.reportYear"> · {{ item.reportYear }} 年报</template>
                  <template v-if="item.sourceLabel"> · {{ item.sourceLabel }}</template>
                </span>
              </div>
              <span class="history-time">{{ relativeTime(item.timestamp) }}</span>
              <button class="history-del" @click.stop="remove(item.code)">×</button>
            </div>
            <div v-if="items.length > 0" class="history-footer">
              <button @click="clear()">清空全部历史</button>
            </div>
          </div>
        </div>
        <!-- Phase 4: 登录/用户按钮 -->
        <button v-if="!auth.isLoggedIn" class="auth-btn" @click="router.push('/login')">
          🔑 登录
        </button>
        <div v-else class="user-info">
          <span class="user-name">{{ auth.user?.nickname || auth.user?.email }}</span>
          <button class="auth-btn" @click="handleLogout">退出</button>
        </div>
      </div>
    </div>
    <ReportLibraryModal :open="showLibrary" @close="showLibrary = false" />
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  height: 64px;
}
.navbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  height: 100%;
  gap: 20px;
}
.navbar-left { flex-shrink: 0; display: flex; align-items: center; gap: 12px; }
.brand {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  cursor: pointer;
  user-select: none;
}
.library-btn {
  padding: 7px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: var(--surface-alt);
  color: var(--text-secondary);
  font-size: 13px;
}
.library-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--primary-light); }
.navbar-center { flex: 1; display: flex; justify-content: center; }
.navbar-right { flex-shrink: 0; display: flex; align-items: center; gap: 8px; }

.history-wrapper { position: relative; }
.history-btn {
  padding: 8px 14px;
  background: var(--surface-alt);
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: background 0.2s;
}
.history-btn:hover { background: var(--hover); }

.history-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 300px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  z-index: 200;
  overflow: hidden;
}
.history-empty {
  padding: 24px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}
.history-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  gap: 8px;
}
.history-item:hover { background: var(--hover); }
.history-item-main {
  flex: 1;
  min-width: 0;
}
.history-name {
  font-size: 14px;
  font-weight: 500;
  display: block;
}
.history-code {
  font-size: 12px;
  color: var(--text-muted);
}
.history-time {
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}
.history-del {
  font-size: 18px;
  color: var(--text-muted);
  background: none;
  padding: 2px 6px;
  border-radius: 4px;
}
.history-del:hover { color: var(--risk-red); background: var(--risk-red-bg); }
.history-footer {
  border-top: 1px solid var(--border);
  button {
    width: 100%;
    padding: 10px;
    background: none;
    color: var(--risk-red);
    font-size: 13px;
    &:hover { background: var(--risk-red-bg); }
  }
}

/* Phase 4: 认证按钮 */
.auth-btn {
  padding: 8px 14px;
  background: var(--primary, #3b82f6);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.15s;
}
.auth-btn:hover { opacity: 0.85; }
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  font-size: 13px;
  color: var(--text);
  font-weight: 500;
}

@media (max-width: 768px) {
  .navbar-center { display: none; }
  .navbar-inner { gap: 10px; }
  .user-name { display: none; }
}
</style>
