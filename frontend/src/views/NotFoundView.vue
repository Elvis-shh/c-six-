<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面未找到</p>
    <router-link to="/search">返回首页</router-link>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
}
.not-found h1 { font-size: 64px; color: var(--text-muted); }
.not-found p { font-size: 18px; color: var(--text-secondary); margin: 12px 0 24px; }
</style>
