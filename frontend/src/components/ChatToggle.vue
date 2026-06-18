<script setup lang="ts">
import { ref } from 'vue'
import { useChatStore } from '@/stores'

const store = useChatStore()
const position = ref({ right: 24, bottom: 24 })
let startX = 0
let startY = 0
let startRight = 0
let startBottom = 0
let dragging = false

function onPointerDown(event: PointerEvent) {
  startX = event.clientX
  startY = event.clientY
  startRight = position.value.right
  startBottom = position.value.bottom
  dragging = false
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function onPointerMove(event: PointerEvent) {
  const dx = event.clientX - startX
  const dy = event.clientY - startY
  if (Math.abs(dx) > 4 || Math.abs(dy) > 4) dragging = true
  position.value = {
    right: Math.max(12, startRight - dx),
    bottom: Math.max(12, startBottom - dy),
  }
}

function onPointerUp() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', onPointerUp)
  if (!dragging) store.toggle()
}
</script>

<template>
  <button
    v-if="!store.isOpen"
    class="chat-toggle"
    :style="{ right: `${position.right}px`, bottom: `${position.bottom}px` }"
    aria-label="打开 AI 财报助手"
    @pointerdown.prevent="onPointerDown"
  >
    AI
  </button>
</template>

<style scoped>
.chat-toggle {
  position: fixed;
  z-index: 1000;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary), #7c3aed);
  box-shadow: var(--shadow-lg);
  cursor: grab;
  touch-action: none;
}
</style>
