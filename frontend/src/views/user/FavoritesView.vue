<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { listFavorites } from '@/api/favorite'
import type { Dish } from '@/api/types'
import DishCard from '@/components/DishCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const dishes = ref<Dish[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    dishes.value = await listFavorites()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100">
    <header class="pb-safe-2 sticky top-0 z-30 flex items-center justify-between border-b border-cream-300 bg-cream-100/95 px-4 py-3 pt-safe backdrop-blur">
      <button type="button" aria-label="返回" class="text-cocoa-500" @click="router.back()">
        <Icon icon="ph:caret-left" width="28" />
      </button>
      <h1 class="text-base font-bold text-cocoa-700">我的收藏</h1>
      <span class="w-7" />
    </header>

    <div class="px-4 py-4 pb-24">
      <div v-if="loading" class="h-40 animate-pulse rounded-2xl bg-white" />
      <EmptyState
        v-else-if="dishes.length === 0"
        icon="ph:star"
        text="还没有收藏菜品"
        hint="在菜品详情里点小心心，把爱吃的收藏起来吧"
      >
        <button type="button" class="btn-primary mt-2" @click="router.replace('/home')">
          <Icon icon="ph:bowl-food" /> 去点菜
        </button>
      </EmptyState>
      <div v-else class="space-y-3">
        <DishCard v-for="dish in dishes" :key="dish.id" :dish="dish" />
      </div>
    </div>
  </div>
</template>
