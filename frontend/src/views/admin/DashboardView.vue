<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, ref } from 'vue'

import { getDashboard } from '@/api/admin'
import type { Dashboard } from '@/api/types'

const data = ref<Dashboard | null>(null)
const loading = ref(true)

const stats = computed(() => {
  if (!data.value) return []
  return [
    {
      label: '今日订单',
      value: data.value.todayOrderCount,
      icon: 'ph:bowl-food',
      cls: 'bg-primary-100 text-primary-500',
    },
    {
      label: '待处理',
      value: data.value.pendingCount,
      icon: 'ph:bell-ringing',
      cls: 'bg-berry-50 text-berry-500',
    },
    {
      label: '今日已完成',
      value: data.value.todayCompletedCount,
      icon: 'ph:check-circle',
      cls: 'bg-honey-100 text-honey-600',
    },
    {
      label: '累计订单',
      value: data.value.totalCount,
      icon: 'ph:stack',
      cls: 'bg-cream-200 text-cocoa-600',
    },
  ]
})

/** 柱状图最大值，用于换算柱高百分比 */
const trendMax = computed(() =>
  Math.max(1, ...(data.value?.trend.map((t) => t.count) ?? [1])),
)

const maxTop = computed(() =>
  Math.max(1, ...(data.value?.topDishes.map((t) => t.count) ?? [1])),
)

onMounted(async () => {
  try {
    data.value = await getDashboard()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="space-y-5">
    <!-- 指标卡 -->
    <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
      <div v-for="stat in stats" :key="stat.label" class="card flex items-center gap-3 p-4">
        <div
          :class="['flex h-11 w-11 flex-none items-center justify-center rounded-xl', stat.cls]"
        >
          <Icon :icon="stat.icon" width="24" />
        </div>
        <div>
          <p class="text-2xl font-bold leading-none text-cocoa-700">{{ stat.value }}</p>
          <p class="mt-1 text-xs text-cocoa-400">{{ stat.label }}</p>
        </div>
      </div>
    </div>

    <div class="grid gap-5 lg:grid-cols-5">
      <!-- 7 天趋势 -->
      <div class="card p-5 lg:col-span-3">
        <div class="mb-5 flex items-center justify-between">
          <p class="text-sm font-bold text-cocoa-700">最近 7 天订单趋势</p>
          <span class="text-xs text-cocoa-300">单位：单</span>
        </div>

        <div class="flex h-48 items-end justify-between gap-2">
          <div
            v-for="trend in data?.trend"
            :key="trend.date"
            class="flex flex-1 flex-col items-center justify-end gap-2"
          >
            <span class="text-xs font-medium text-cocoa-600">{{ trend.count }}</span>
            <div
              class="w-full max-w-[36px] rounded-t-lg bg-gradient-to-t from-primary-400 to-primary-300 transition-all"
              :style="{ height: `${(trend.count / trendMax) * 100}%`, minHeight: trend.count ? '8px' : '2px' }"
              :class="trend.count ? '' : 'opacity-30'"
            />
            <span class="text-[10px] text-cocoa-400">
              {{ trend.date.slice(5).replace('-', '/') }}
            </span>
          </div>
        </div>
      </div>

      <!-- Top5 -->
      <div class="card p-5 lg:col-span-2">
        <p class="mb-4 text-sm font-bold text-cocoa-700">宝贝最常点 Top 5</p>
        <div v-if="(data?.topDishes.length ?? 0) === 0" class="py-10 text-center text-xs text-cocoa-300">
          还没有订单数据
        </div>
        <div v-else class="space-y-3.5">
          <div v-for="(dish, index) in data?.topDishes" :key="dish.dishId" class="flex items-center gap-3">
            <span
              class="flex h-6 w-6 flex-none items-center justify-center rounded-full text-xs font-bold"
              :class="
                index === 0
                  ? 'bg-berry-400 text-white'
                  : index < 3
                    ? 'bg-primary-200 text-primary-500'
                    : 'bg-cream-200 text-cocoa-500'
              "
              >{{ index + 1 }}</span
            >
            <span class="flex-1 truncate text-sm text-cocoa-600">{{ dish.dishName }}</span>
            <div class="h-2 w-20 overflow-hidden rounded-full bg-cream-200">
              <div
                class="h-full rounded-full bg-gradient-to-r from-primary-300 to-berry-400"
                :style="{ width: `${(dish.count / maxTop) * 100}%` }"
              />
            </div>
            <span class="w-8 text-right text-xs text-cocoa-500">{{ dish.count }}次</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
