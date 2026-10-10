<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, ref } from 'vue'

import { listCategories, listRecommend, pageDishes } from '@/api/catalog'
import { getPublicConfig } from '@/api/config'
import type { Category, Dish } from '@/api/types'
import DishCard from '@/components/DishCard.vue'
import DishSkeleton from '@/components/DishSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { chineseDate, debounce, greeting } from '@/utils/datetime'
import { randomLoveText } from '@/utils/love-texts'

const userStore = useUserStore()

const categories = ref<Category[]>([])
const dishes = ref<Dish[]>([])
const activeTab = ref<string>('recommend')
const loading = ref(false)
const keyword = ref('')
const notice = ref('')
const businessOpen = ref(true)
const loveText = ref(randomLoveText())

const tabs = computed(() => [
  { id: 'recommend', name: '推荐' },
  ...categories.value.map((category) => ({ id: String(category.id), name: category.name })),
])

async function loadDishes() {
  loading.value = true
  try {
    if (keyword.value.trim()) {
      const page = await pageDishes({
        keyword: keyword.value.trim(),
        pageNum: 1,
        pageSize: 100,
      })
      dishes.value = page.list
      return
    }
    if (activeTab.value === 'recommend') {
      dishes.value = await listRecommend(8)
    } else {
      const page = await pageDishes({
        categoryId: Number(activeTab.value),
        pageNum: 1,
        pageSize: 100,
      })
      dishes.value = page.list
    }
  } finally {
    loading.value = false
  }
}

function selectTab(id: string) {
  activeTab.value = id
  keyword.value = ''
  loadDishes()
}

const onSearch = debounce(() => {
  loadDishes()
}, 300)

onMounted(async () => {
  const [config, cats] = await Promise.all([getPublicConfig(), listCategories()])
  notice.value = config.notice
  businessOpen.value = config.businessOpen === 1
  categories.value = cats
  await loadDishes()
})
</script>

<template>
  <div class="pb-24">
    <!-- 顶部问候 -->
    <header class="bg-gradient-to-b from-primary-100 to-cream-100 px-5 pb-5 pt-6 pt-safe">
      <div class="flex items-center justify-between">
        <div>
          <p class="text-lg font-bold text-cocoa-700">
            {{ greeting() }}，{{ userStore.nickname }}
          </p>
          <p class="mt-0.5 text-xs text-cocoa-500">{{ chineseDate() }}</p>
        </div>
        <div
          class="flex h-12 w-12 items-center justify-center rounded-full bg-white shadow-card"
        >
          <Icon icon="ph:bowl-food" class="text-primary-400" width="26" />
        </div>
      </div>

      <!-- 情话卡片 -->
      <div class="mt-4 flex items-start gap-2 rounded-2xl bg-white/80 p-3 shadow-card">
        <Icon icon="ph:heart-straight-fill" class="mt-0.5 flex-none text-berry-400" width="18" />
        <p class="text-sm leading-relaxed text-cocoa-600">{{ loveText }}</p>
      </div>

      <!-- 公告 -->
      <div v-if="notice" class="mt-2 flex items-start gap-2 rounded-2xl bg-honey-100 p-3">
        <Icon icon="ph:megaphone" class="mt-0.5 flex-none text-honey-500" width="18" />
        <p class="text-sm leading-relaxed text-cocoa-600">{{ notice }}</p>
      </div>

      <!-- 歇业提示 -->
      <div
        v-if="!businessOpen"
        class="mt-2 flex items-center gap-2 rounded-2xl bg-cocoa-700 p-3 text-white"
      >
        <Icon icon="ph:moon-stars" width="18" />
        <p class="text-sm">今日暂不营业，大厨休息中，明天见呀~</p>
      </div>
    </header>

    <!-- 搜索 -->
    <div class="sticky top-0 z-30 bg-cream-100/95 px-5 py-2 backdrop-blur">
      <div class="relative">
        <Icon
          icon="ph:magnifying-glass"
          class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-cocoa-300"
          width="18"
        />
        <input
          v-model="keyword"
          class="input py-2.5 pl-10"
          placeholder="搜索想吃的菜…"
          @input="onSearch"
        />
        <button
          v-if="keyword"
          type="button"
          aria-label="清除搜索"
          class="absolute right-3 top-1/2 -translate-y-1/2 text-cocoa-300"
          @click="keyword = ''; loadDishes()"
        >
          <Icon icon="ph:x-circle" width="18" />
        </button>
      </div>

      <!-- 分类 Tab（横向滚动） -->
      <div class="no-scrollbar -mx-5 mt-2 flex gap-2 overflow-x-auto px-5">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="flex-none rounded-pill px-4 py-1.5 text-sm transition"
          :class="
            activeTab === tab.id && !keyword
              ? 'bg-primary-400 text-white shadow-warm'
              : 'bg-white text-cocoa-500'
          "
          @click="selectTab(tab.id)"
        >
          {{ tab.name }}
        </button>
      </div>
    </div>

    <!-- 菜品列表 -->
    <div class="px-4 pt-2">
      <DishSkeleton v-if="loading" />
      <EmptyState
        v-else-if="dishes.length === 0"
        icon="ph:fork-knife"
        :text="keyword ? '没有找到这道菜，换个关键词试试？' : '这里暂时还没有菜品'"
        :hint="keyword ? '或者问问大厨能不能做~' : ''"
      />
      <div v-else class="space-y-3">
        <DishCard v-for="dish in dishes" :key="dish.id" :dish="dish" />
      </div>
    </div>
  </div>
</template>
