<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { cancelOrder, pageMyOrders } from '@/api/order'
import type { Order } from '@/api/types'
import EmptyState from '@/components/EmptyState.vue'
import { ACTIVE_STATUSES } from '@/utils/datetime'
import { toast } from '@/utils/toast'

const router = useRouter()

const tab = ref<'active' | 'finished'>('active')
const orders = ref<Order[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = 20
let pollTimer: number | undefined

const hasMore = computed(() => orders.value.length < total.value)

/** 状态徽标配色 */
function statusClass(status: string) {
  if (status === 'COMPLETED') return 'bg-primary-100 text-primary-500'
  if (status === 'PENDING') return 'bg-honey-100 text-honey-600'
  if (['ACCEPTED', 'COOKING'].includes(status)) return 'bg-berry-50 text-berry-500'
  return 'bg-cream-200 text-cocoa-400'
}

async function load(reset = false) {
  loading.value = true
  if (reset) {
    pageNum.value = 1
  }
  try {
    const page = await pageMyOrders({
      tab: tab.value,
      pageNum: pageNum.value,
      pageSize,
    })
    total.value = page.total
    orders.value = reset ? page.list : [...orders.value, ...page.list]
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  pageNum.value += 1
  await load()
}

async function handleCancel(order: Order) {
  await cancelOrder(order.id)
  toast('订单已取消')
  await load(true)
}

/** 启动 10s 轮询（仅进行中 Tab） */
function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    // 静默刷新，不弹 loading
    const page = await pageMyOrders({ tab: 'active', pageNum: 1, pageSize })
    total.value = page.total
    orders.value = page.list
  }, 10000)
}

function stopPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

watch(tab, async () => {
  await load(true)
  if (tab.value === 'active') startPolling()
  else stopPolling()
})

onMounted(async () => {
  await load(true)
  startPolling()
})

onUnmounted(stopPolling)
</script>

<template>
  <div class="pb-24">
    <header class="px-5 pb-2 pt-6 pt-safe">
      <h1 class="text-xl font-bold text-cocoa-700">我的订单</h1>
    </header>

    <!-- Tab 切换 -->
    <div class="mx-5 mb-3 flex rounded-pill bg-white p-1">
      <button
        type="button"
        class="flex-1 rounded-pill py-2 text-sm transition"
        :class="tab === 'active' ? 'bg-primary-400 text-white shadow-warm' : 'text-cocoa-500'"
        @click="tab = 'active'"
      >
        进行中
      </button>
      <button
        type="button"
        class="flex-1 rounded-pill py-2 text-sm transition"
        :class="tab === 'finished' ? 'bg-primary-400 text-white shadow-warm' : 'text-cocoa-500'"
        @click="tab = 'finished'"
      >
        已完成
      </button>
    </div>

    <div class="px-4">
      <EmptyState
        v-if="!loading && orders.length === 0"
        icon="ph:receipt"
        :text="tab === 'active' ? '当前没有进行中的订单' : '还没有历史订单'"
        :hint="tab === 'active' ? '点好的菜会出现在这里~' : ''"
      >
        <button
          v-if="tab === 'active'"
          type="button"
          class="btn-primary mt-2"
          @click="router.replace('/home')"
        >
          <Icon icon="ph:bowl-food" /> 去点菜
        </button>
      </EmptyState>

      <div v-else class="space-y-3">
        <div
          v-for="order in orders"
          :key="order.id"
          class="card cursor-pointer p-4 transition active:scale-[0.99]"
          @click="router.push(`/orders/${order.id}`)"
        >
          <div class="flex items-center justify-between">
            <span class="text-xs text-cocoa-400">单号 {{ order.orderNo }}</span>
            <span
              class="rounded-pill px-2.5 py-0.5 text-[11px]"
              :class="statusClass(order.status)"
            >
              {{ order.statusLabel }}
            </span>
          </div>

          <div class="mt-2 flex items-center gap-2">
            <img
              v-for="item in (order.items || []).slice(0, 4)"
              :key="item.dishId + item.taste"
              :src="item.dishImage"
              :alt="item.dishName"
              class="h-10 w-10 rounded-lg bg-cream-200 object-cover"
            />
            <div v-if="(order.items || []).length > 4"
              class="flex h-10 w-10 items-center justify-center rounded-lg bg-cream-200 text-xs text-cocoa-500"
            >
              +{{ (order.items || []).length - 4 }}
            </div>
          </div>

          <p class="mt-2 truncate text-sm text-cocoa-600">
            {{ (order.items || []).map((item) => item.dishName).join('、') }}
          </p>
          <p v-if="order.remark" class="mt-0.5 truncate text-xs text-cocoa-400">
            备注：{{ order.remark }}
          </p>

          <div class="mt-3 flex items-center justify-between border-t border-dashed border-cream-300 pt-3">
            <span class="text-xs text-cocoa-400">{{ order.createTime }}</span>
            <div class="flex items-center gap-3">
              <button
                v-if="order.status === 'PENDING'"
                type="button"
                class="rounded-pill border border-berry-200 px-3 py-1 text-xs text-berry-500"
                @click.stop="handleCancel(order)"
              >
                取消订单
              </button>
              <span class="flex items-center gap-0.5 text-sm font-bold text-primary-500">
                <Icon icon="ph:heart-fill" width="14" />
                {{ order.totalLove }}
              </span>
            </div>
          </div>
        </div>

        <button
          v-if="hasMore"
          type="button"
          class="w-full rounded-pill bg-white py-2.5 text-sm text-cocoa-500"
          @click="loadMore"
        >
          {{ loading ? '加载中…' : '加载更多' }}
        </button>
      </div>
    </div>
  </div>
</template>
