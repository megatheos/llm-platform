<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AppLayout from '@/components/AppLayout.vue'

const route = useRoute()

const useAppLayout = computed(() => {
  return route.meta.layout === 'app'
})
</script>

<template>
  <AppLayout v-if="useAppLayout">
    <RouterView />
  </AppLayout>
  <RouterView v-else />
</template>

<style>
#app {
  font-family: var(--font-family);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
