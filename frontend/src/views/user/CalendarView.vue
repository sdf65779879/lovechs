<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getCalendar } from '@/api/order'
import type { CalendarDay } from '@/api/types'
import { formatDate, monthString } from '@/utils/datetime'

const router = useRouter()

const cursor = ref(new Date())
const dayMap = ref<Map<string, CalendarDay>>(new Map())
const selectedDate = ref<string>('')
const loading = ref(false)

const WEEK_HEADERS = ['一', '二', '三', '四', '五', '六', '日']

/** 日历格子：前置空位 + 当月每天 */
const cells = computed<(string | null)[]>(() => {
  const year = cursor.value.getFullYear()
  const month = cursor.value.getMonth()
  const firstDay = new Date(year, month, 1)
  // 周一为一周起点
  const leading = (firstDay.getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const result: (string | null)[] = Array(leading).fill(null)
  for (let day = 1; day <= daysInMonth; day += 1) {
    result.push(formatDate(new Date(year, month, day)))
  }
  return result
})

const title = computed(() => {
  return `${cursor.value.getFullYear()}年${cursor.value.getMonth() + 1}月`
})

const selectedDay = computed(() =>
  selectedDate.value ? dayMap.value.get(selectedDate.value) : undefined,
)

async function loadMonth() {
  loading.value = true
  try {
    const calendar = await getCalendar(monthString(cursor.value))
    const map = new Map<string, CalendarDay>()
    calendar.days.forEach((day) => map.set(day.date, day))
    dayMap.value = map
  } finally {
    loading.value = false
  }
}

function prevMonth() {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() - 1, 1)
  selectedDate.value = ''
}

function nextMonth() {
  cursor.value = new Date(cursor.value.getFullYear(), cursor.value.getMonth() + 1, 1)
  selectedDate.value = ''
}

function selectDay(date: string) {
  selectedDate.value = selectedDate.value === date ? '' : date
}

watch(cursor, loadMonth)
onMounted(loadMonth)
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100">
    <header class="pb-safe-2 sticky top-0 z-30 flex items-center justify-between border-b border-cream-300 bg-cream-100/95 px-4 py-3 pt-safe backdrop-blur">
      <button type="button" aria-label="返回" class="text-cocoa-500" @click="router.back()">
        <Icon icon="ph:caret-left" width="28" />
      </button>
      <h1 class="text-base font-bold text-cocoa-700">吃饭日历</h1>
      <span class="w-7" />
    </header>

    <div class="p-4">
      <div class="card p-4">
        <!-- 月份切换 -->
        <div class="mb-4 flex items-center justify-between">
          <button type="button" class="text-cocoa-400" aria-label="上个月" @click="prevMonth">
            <Icon icon="ph:caret-left" width="24" />
          </button>
          <p class="text-sm font-bold text-cocoa-700">{{ title }}</p>
          <button type="button" class="text-cocoa-400" aria-label="下个月" @click="nextMonth">
            <Icon icon="ph:caret-right" width="24" />
          </button>
        </div>

        <!-- 星期表头 -->
        <div class="mb-1 grid grid-cols-7 text-center text-[11px] text-cocoa-300">
          <span v-for="week in WEEK_HEADERS" :key="week" class="py-1">{{ week }}</span>
        </div>

        <!-- 日期格子 -->
        <div class="grid grid-cols-7 gap-y-1 text-center">
          <template v-for="(cell, index) in cells" :key="index">
            <div v-if="cell === null" />
            <button
              v-else
              type="button"
              class="relative mx-auto flex h-9 w-9 flex-col items-center justify-center rounded-full text-sm transition"
              :class="[
                selectedDate === cell
                  ? 'bg-primary-400 text-white'
                  : dayMap.has(cell)
                    ? 'bg-primary-100 text-primary-500'
                    : 'text-cocoa-500',
              ]"
              @click="selectDay(cell)"
            >
              {{ Number(cell.slice(8)) }}
              <!-- 有订单打点 -->
              <span
                v-if="dayMap.has(cell)"
                class="absolute bottom-1 h-1 w-1 rounded-full"
                :class="selectedDate === cell ? 'bg-white' : 'bg-berry-400'"
              />
            </button>
          </template>
        </div>
      </div>

      <!-- 选中日期的订单 -->
      <div v-if="selectedDay" class="card mt-3 p-4">
        <div class="mb-3 flex items-center justify-between">
          <p class="text-sm font-bold text-cocoa-700">{{ selectedDate }}</p>
          <p class="flex items-center gap-1 text-xs text-primary-500">
            <Icon icon="ph:heart-fill" width="12" />
            {{ selectedDay.totalLove }} · {{ selectedDay.orderCount }} 单
          </p>
        </div>
        <div class="space-y-2">
          <button
            v-for="order in selectedDay.orders"
            :key="order.id"
            type="button"
            class="flex w-full items-center justify-between rounded-pill bg-cream-100 px-4 py-2 text-sm"
            @click="router.push(`/orders/${order.id}`)"
          >
            <span class="text-cocoa-600">{{ order.orderNo }}</span>
            <span class="flex items-center gap-2">
              <span class="text-xs text-cocoa-400">{{ order.statusLabel }}</span>
              <Icon icon="ph:caret-right" class="text-cocoa-300" />
            </span>
          </button>
        </div>
      </div>

      <p v-else class="mt-4 text-center text-xs text-cocoa-300">
        点击带圆点的日期，回看那天一起吃过的饭
      </p>
    </div>
  </div>
</template>
