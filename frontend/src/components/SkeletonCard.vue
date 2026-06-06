<template>
  <div class="skeleton-card">
    <div class="skeleton-line skeleton-title" />
    <div class="skeleton-line skeleton-value" />
    <div class="skeleton-line skeleton-sub" />
  </div>
</template>

<style scoped>
.skeleton-card {
  padding: 24px;
  background: var(--surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.skeleton-line {
  height: 16px;
  background: linear-gradient(
    90deg,
    var(--skeleton-base) 25%,
    var(--skeleton-shine) 50%,
    var(--skeleton-base) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 10px;
}
.skeleton-title { width: 55%; }
.skeleton-value { width: 35%; height: 32px; }
.skeleton-sub { width: 70%; }
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
