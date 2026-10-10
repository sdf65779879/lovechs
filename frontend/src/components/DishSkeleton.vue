<script setup lang="ts">
withDefaults(defineProps<{ count?: number }>(), { count: 4 })
</script>

<template>
  <div class="space-y-3">
    <div
      v-for="n in count"
      :key="n"
      class="flex animate-pulse gap-3 rounded-2xl bg-white p-3"
    >
      <div class="h-20 w-24 flex-none rounded-xl bg-cream-200" />
      <div class="flex-1 space-y-2 py-1">
        <div class="h-4 w-2/5 rounded bg-cream-200" />
        <div class="h-3 w-full rounded bg-cream-200" />
        <div class="h-3 w-4/5 rounded bg-cream-200" />
        <div class="h-4 w-1/4 rounded bg-cream-200" />
      </div>
    </div>
  </div>
</template>
