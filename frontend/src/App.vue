<template>
  <div id="app-root">
    <Navbar v-if="showNavbar" />
    <main class="main-content" :class="{ 'has-navbar': showNavbar }">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Navbar from '@/components/Navbar.vue'

const route = useRoute()
const showNavbar = computed(() => route.name !== 'Login')
</script>

<style scoped>
.main-content {
  min-height: 100vh;
}
.main-content.has-navbar {
  padding-top: 64px;
}
</style>
