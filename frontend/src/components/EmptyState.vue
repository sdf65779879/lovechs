<script setup lang="ts">
import { Icon } from '@iconify/vue'

withDefaults(
  defineProps<{
    icon?: string
    text: string
    hint?: string
  }>(),
  { icon: 'ph:package', hint: '' },
)
</script>

<template>
  <div class="flex flex-col items-center gap-3 px-8 py-16 text-center">
    <div class="flex h-20 w-20 items-center justify-center rounded-full bg-cream-200 text-cocoa-300">
      <Icon :icon="icon" width="40" />
    </div>
    <p class="text-sm font-medium text-cocoa-500">{{ text }}</p>
    <p v-if="hint" class="text-xs text-cocoa-300">{{ hint }}</p>
    <slot />
  </div>
</template>
