<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, ref, watch } from 'vue'

import { getDish } from '@/api/catalog'
import type { Dish } from '@/api/types'
import QuantityStepper from '@/components/QuantityStepper.vue'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { toast } from '@/utils/toast'

const cartStore = useCartStore()
const uiStore = useUiStore()

const dish = ref<Dish | null>(null)
const loading = ref(false)
const quantity = ref(1)
const taste = ref('')
const remark = ref('')
const adding = ref(false)

const SPICY_OPTIONS = ['不辣', '微辣', '中辣', '变态辣']
const SWEET_OPTIONS = ['正常甜', '少糖', '半糖', '全糖']
/** 甜品(4)、饮品(5)分类默认提供甜度选择 */
const SWEET_CATEGORY_IDS = [4, 5]

const showSpicy = computed(() => {
  if (!dish.value) return false
  return (
    dish.value.tagList.some((tag) => tag.includes('辣')) ||
    dish.value.name.includes('辣')
  )
})

const showSweet = computed(() => {
  if (!dish.value) return false
  return (
    SWEET_CATEGORY_IDS.includes(dish.value.categoryId) ||
    dish.value.tagList.some((tag) => tag.includes('甜')) ||
    dish.value.name.includes('甜')
  )
})

watch(
  () => uiStore.detailDishId,
  async (id) => {
    if (!id) {
      dish.value = null
      return
    }
    loading.value = true
    quantity.value = 1
    taste.value = ''
    remark.value = ''
    try {
      dish.value = await getDish(id)
    } catch {
      uiStore.closeDishDetail()
    } finally {
      loading.value = false
    }
  },
)

async function handleAdd() {
  if (!dish.value) return
  adding.value = true
  try {
    await cartStore.addToCart({
      dishId: dish.value.id,
      quantity: quantity.value,
      taste: taste.value,
      remark: remark.value.trim(),
    })
    toast(`已加入 ${quantity.value} 份${dish.value.name}`)
    uiStore.closeDishDetail()
  } finally {
    adding.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <transition name="sheet-fade">
      <div
        v-if="uiStore.detailDishId"
        class="fixed inset-0 z-50 bg-cocoa-900/40"
        @click="uiStore.closeDishDetail()"
      />
    </transition>

    <transition name="sheet-slide">
      <div
        v-if="uiStore.detailDishId"
        class="pb-safe fixed bottom-0 left-0 right-0 z-50 mx-auto flex max-h-[88vh] w-full max-w-lg flex-col overflow-hidden rounded-t-3xl bg-cream-100 shadow-warm"
      >
        <!-- 关闭按钮 -->
        <button
          type="button"
          aria-label="关闭"
          class="absolute right-4 top-4 z-10 flex h-9 w-9 items-center justify-center rounded-full bg-white/80 text-cocoa-500"
          @click="uiStore.closeDishDetail()"
        >
          <Icon icon="ph:x" width="20" />
        </button>

        <div v-if="loading" class="h-64 animate-pulse bg-cream-200" />

        <template v-else-if="dish">
          <!-- 大图 -->
          <img
            :src="dish.image"
            :alt="dish.name"
            class="h-60 w-full flex-none bg-cream-200 object-cover"
          />

          <div class="flex-1 overflow-y-auto px-5 pb-4 pt-4">
            <div class="flex items-start justify-between gap-3">
              <h2 class="text-xl font-bold text-cocoa-700">{{ dish.name }}</h2>
              <span class="flex flex-none items-center gap-1 text-primary-500">
                <Icon icon="ph:heart-fill" />
                <span class="text-lg font-bold">{{ dish.loveValue }}</span>
              </span>
            </div>

            <p class="mt-2 text-sm leading-relaxed text-cocoa-500">
              {{ dish.description }}
            </p>

            <div class="mt-2 flex flex-wrap gap-1.5">
              <span v-for="tag in dish.tagList" :key="tag" class="tag">{{ tag }}</span>
            </div>

            <!-- 辣度 -->
            <div v-if="showSpicy" class="mt-4">
              <p class="mb-2 text-sm font-medium text-cocoa-700">辣度</p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="option in SPICY_OPTIONS"
                  :key="option"
                  type="button"
                  class="rounded-pill border px-4 py-1.5 text-sm transition"
                  :class="
                    taste === option
                      ? 'border-primary-400 bg-primary-400 text-white'
                      : 'border-cream-300 bg-white text-cocoa-500'
                  "
                  @click="taste = taste === option ? '' : option"
                >
                  {{ option }}
                </button>
              </div>
            </div>

            <!-- 甜度 -->
            <div v-if="showSweet" class="mt-4">
              <p class="mb-2 text-sm font-medium text-cocoa-700">甜度</p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="option in SWEET_OPTIONS"
                  :key="option"
                  type="button"
                  class="rounded-pill border px-4 py-1.5 text-sm transition"
                  :class="
                    taste === option
                      ? 'border-berry-400 bg-berry-400 text-white'
                      : 'border-cream-300 bg-white text-cocoa-500'
                  "
                  @click="taste = taste === option ? '' : option"
                >
                  {{ option }}
                </button>
              </div>
            </div>

            <!-- 单品备注 -->
            <div class="mt-4">
              <p class="mb-2 text-sm font-medium text-cocoa-700">
                单品备注 <span class="font-normal text-cocoa-300">（忌口 / 悄悄话）</span>
              </p>
              <textarea
                v-model="remark"
                rows="2"
                maxlength="100"
                class="input resize-none"
                placeholder="比如：不要香菜、少放盐，或者……夸夸大厨"
              />
            </div>
          </div>

          <!-- 底部操作 -->
          <div class="flex items-center gap-4 border-t border-cream-300 px-5 py-3">
            <QuantityStepper v-model="quantity" />
            <button
              type="button"
              class="btn-primary flex-1 py-3"
              :disabled="adding"
              @click="handleAdd"
            >
              <Icon icon="ph:shopping-cart" />
              {{ adding ? '加入中…' : '加入购物车' }}
            </button>
          </div>
        </template>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.sheet-fade-enter-active,
.sheet-fade-leave-active {
  transition: opacity 0.2s ease;
}
.sheet-fade-enter-from,
.sheet-fade-leave-to {
  opacity: 0;
}
.sheet-slide-enter-active,
.sheet-slide-leave-active {
  transition: transform 0.25s ease;
}
.sheet-slide-enter-from,
.sheet-slide-leave-to {
  transform: translateY(100%);
}
</style>
