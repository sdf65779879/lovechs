<script setup lang="ts">
import { Icon } from '@iconify/vue'

import type { Dish } from '@/api/types'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { toast } from '@/utils/toast'

const props = defineProps<{ dish: Dish }>()

const cartStore = useCartStore()
const uiStore = useUiStore()

/** 标签配色：辣→橘，甜→粉，拿手→蜂蜜，其余默认 */
function tagClass(tag: string) {
  if (tag.includes('辣')) return 'tag-spicy'
  if (tag.includes('甜')) return 'tag-berry'
  if (tag.includes('拿手') || tag.includes('夜宵')) return 'tag-honey'
  return ''
}

async function quickAdd() {
  await cartStore.addToCart({ dishId: props.dish.id, quantity: 1 })
  toast('已加入购物车')
}
</script>

<template>
  <div class="card relative overflow-hidden">
    <button
      type="button"
      class="flex w-full gap-3 p-3 text-left"
      @click="uiStore.openDishDetail(dish.id)"
    >
      <img
        :src="dish.image"
        :alt="dish.name"
        loading="lazy"
        class="h-20 w-24 flex-none rounded-xl bg-cream-200 object-cover"
      />
      <div class="min-w-0 flex-1 pr-9">
        <div class="flex items-center gap-1.5">
          <h3 class="truncate font-medium text-cocoa-700">{{ dish.name }}</h3>
        </div>
        <p class="mt-0.5 line-clamp-2 text-xs leading-relaxed text-cocoa-500">
          {{ dish.description }}
        </p>
        <div class="mt-1.5 flex flex-wrap gap-1">
          <span
            v-for="tag in dish.tagList.slice(0, 3)"
            :key="tag"
            class="tag"
            :class="tagClass(tag)"
            >{{ tag }}</span
          >
        </div>
        <div class="mt-1.5 flex items-center gap-0.5 text-sm font-medium text-primary-500">
          <Icon icon="ph:heart-fill" width="14" />
          {{ dish.loveValue }}
        </div>
      </div>
    </button>

    <button
      type="button"
      aria-label="加入购物车"
      class="absolute bottom-3 right-3 flex h-8 w-8 items-center justify-center rounded-full bg-primary-400 text-white shadow-warm transition active:scale-90"
      @click="quickAdd"
    >
      <Icon icon="ph:plus" width="18" />
    </button>
  </div>
</template>
