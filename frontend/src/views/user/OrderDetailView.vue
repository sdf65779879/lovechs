<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { cancelOrder, getOrder, rebuyOrder } from '@/api/order'
import type { Order } from '@/api/types'
import StatusTimeline from '@/components/StatusTimeline.vue'
import { ACTIVE_STATUSES } from '@/utils/datetime'
import { toast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()

const order = ref<Order | null>(null)
const loading = ref(false)
let pollTimer: number | undefined

const orderId = computed(() => Number(route.params.id))
const isActive = computed(() => order.value && ACTIVE_STATUSES.includes(order.value.status))
const canRebuy = computed(() =>
  order.value ? ['COMPLETED', 'CANCELLED', 'REJECTED'].includes(order.value.status) : false,
)

async function load() {
  loading.value = true
  try {
    order.value = await getOrder(orderId.value)
  } finally {
    loading.value = false
  }
}

async function handleCancel() {
  await cancelOrder(orderId.value)
  toast('订单已取消')
  await load()
}

async function handleRebuy() {
  const result = await rebuyOrder(orderId.value)
  if (result.skippedNames.length) {
    toast(`已加回 ${result.addedCount} 道，${result.skippedNames.join('、')} 已下架`)
  } else {
    toast('已加回购物车，去看看吧~')
  }
  router.push('/cart')
}

onMounted(async () => {
  await load()
  // 进行中订单每 10s 轮询一次
  pollTimer = window.setInterval(async () => {
    const fresh = await getOrder(orderId.value)
    order.value = fresh
    if (!ACTIVE_STATUSES.includes(fresh.status) && pollTimer) {
      window.clearInterval(pollTimer)
      pollTimer = undefined
    }
  }, 10000)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100">
    <header class="pb-safe-2 sticky top-0 z-30 flex items-center justify-between border-b border-cream-300 bg-cream-100/95 px-4 py-3 pt-safe backdrop-blur">
      <button type="button" aria-label="返回" class="text-cocoa-500" @click="router.back()">
        <Icon icon="ph:caret-left" width="28" />
      </button>
      <h1 class="text-base font-bold text-cocoa-700">订单详情</h1>
      <span class="w-7" />
    </header>

    <div v-if="order" class="flex-1 space-y-3 px-4 py-4 pb-28">
      <!-- 状态时间轴 -->
      <div class="card p-4">
        <StatusTimeline :order="order" />
      </div>

      <!-- 菜品明细 -->
      <div class="card p-4">
        <p class="mb-3 text-sm font-bold text-cocoa-700">菜品明细</p>
        <div class="space-y-3">
          <div v-for="item in order.items" :key="item.dishId + item.taste + item.remark"
            class="flex items-center gap-3">
            <img
              :src="item.dishImage"
              :alt="item.dishName"
              class="h-14 w-16 flex-none rounded-xl bg-cream-200 object-cover"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-cocoa-700">{{ item.dishName }}</p>
              <p class="truncate text-xs text-cocoa-400">
                {{ [item.taste, item.remark].filter(Boolean).join(' · ') || '—' }}
              </p>
            </div>
            <span class="text-xs text-cocoa-400">×{{ item.quantity }}</span>
            <span class="flex w-12 flex-none items-center justify-end gap-0.5 text-sm text-primary-500">
              <Icon icon="ph:heart-fill" width="12" />
              {{ item.lineLove ?? item.loveValue * item.quantity }}
            </span>
          </div>
        </div>
        <div class="mt-3 flex items-center justify-between border-t border-dashed border-cream-300 pt-3">
          <span class="text-sm text-cocoa-500">合计爱心值</span>
          <span class="flex items-center gap-1 text-lg font-bold text-primary-500">
            <Icon icon="ph:heart-fill" />
            {{ order.totalLove }}
          </span>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="card space-y-2 p-4 text-sm">
        <div class="flex justify-between">
          <span class="text-cocoa-400">订单编号</span>
          <span class="text-cocoa-600">{{ order.orderNo }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-cocoa-400">下单时间</span>
          <span class="text-cocoa-600">{{ order.createTime }}</span>
        </div>
        <div v-if="order.expectTime" class="flex justify-between">
          <span class="text-cocoa-400">期望用餐</span>
          <span class="text-cocoa-600">{{ order.expectTime }}</span>
        </div>
        <div v-if="order.acceptTime" class="flex justify-between">
          <span class="text-cocoa-400">接单时间</span>
          <span class="text-cocoa-600">{{ order.acceptTime }}</span>
        </div>
        <div v-if="order.finishTime" class="flex justify-between">
          <span class="text-cocoa-400">完成时间</span>
          <span class="text-cocoa-600">{{ order.finishTime }}</span>
        </div>
        <div v-if="order.remark" class="flex-start flex gap-2 border-t border-dashed border-cream-300 pt-2">
          <span class="flex-none text-cocoa-400">备注</span>
          <span class="text-cocoa-600">{{ order.remark }}</span>
        </div>
      </div>
    </div>

    <!-- 底部操作 -->
    <div v-if="order" class="pb-safe fixed bottom-0 left-0 right-0 flex gap-3 border-t border-cream-300 bg-white px-5 py-3">
      <button
        v-if="order.status === 'PENDING'"
        type="button"
        class="btn-ghost flex-1 py-3"
        @click="handleCancel"
      >
        取消订单
      </button>
      <button
        v-if="canRebuy"
        type="button"
        class="btn-primary flex-1 py-3"
        @click="handleRebuy"
      >
        <Icon icon="ph:arrow-counter-clockwise" /> 再来一单
      </button>
    </div>
  </div>
</template>
