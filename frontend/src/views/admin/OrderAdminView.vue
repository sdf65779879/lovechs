<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'

import { adminChangeOrderStatus, adminGetOrder, adminPageOrders } from '@/api/admin'
import type { Order } from '@/api/types'
import { toast } from '@/utils/toast'

const list = ref<Order[]>([])
const total = ref(0)
const loading = ref(true)
const pageNum = ref(1)
const pageSize = 10

const filterStatus = ref('')
const filterDate = ref('')

// 详情弹窗
const detailOpen = ref(false)
const detail = ref<Order | null>(null)
const rejectMode = ref(false)
const rejectReason = ref('')
const busy = ref(false)

const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 'PENDING', label: '待接单' },
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'COOKING', label: '制作中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REJECTED', label: '已拒绝' },
]

function statusClass(status: string) {
  if (status === 'COMPLETED') return 'bg-primary-100 text-primary-500'
  if (status === 'PENDING') return 'bg-honey-100 text-honey-600'
  if (['ACCEPTED', 'COOKING'].includes(status)) return 'bg-berry-50 text-berry-500'
  return 'bg-cream-200 text-cocoa-400'
}

/** 当前详情状态下可执行的主动作 */
const primaryAction = computed<{ status: string; label: string } | null>(() => {
  if (!detail.value) return null
  switch (detail.value.status) {
    case 'PENDING':
      return { status: 'ACCEPTED', label: '接单' }
    case 'ACCEPTED':
      return { status: 'COOKING', label: '开始制作' }
    case 'COOKING':
      return { status: 'COMPLETED', label: '完成订单' }
    default:
      return null
  }
})

async function load() {
  loading.value = true
  try {
    const page = await adminPageOrders({
      status: filterStatus.value || undefined,
      date: filterDate.value || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function totalPages() {
  return Math.max(1, Math.ceil(total.value / pageSize))
}

function goPage(page: number) {
  pageNum.value = Math.min(Math.max(1, page), totalPages())
  load()
}

async function openDetail(id: number) {
  detail.value = await adminGetOrder(id)
  rejectMode.value = false
  rejectReason.value = detail.value.rejectReason || ''
  detailOpen.value = true
}

async function doPrimary() {
  if (!detail.value || !primaryAction.value) return
  busy.value = true
  try {
    detail.value = await adminChangeOrderStatus(detail.value.id, {
      status: primaryAction.value.status,
    })
    toast(`已更新为「${detail.value.statusLabel}」`)
    await load()
  } finally {
    busy.value = false
  }
}

async function doReject() {
  if (!detail.value) return
  if (!rejectReason.value.trim()) {
    toast('请填写拒绝理由')
    return
  }
  busy.value = true
  try {
    detail.value = await adminChangeOrderStatus(detail.value.id, {
      status: 'REJECTED',
      rejectReason: rejectReason.value.trim(),
    })
    toast('订单已拒绝')
    rejectMode.value = false
    await load()
  } finally {
    busy.value = false
  }
}

let pollTimer: number | undefined
onMounted(async () => {
  await load()
  // 10s 轮询，新单自动出现
  pollTimer = window.setInterval(async () => {
    const page = await adminPageOrders({
      status: filterStatus.value || undefined,
      date: filterDate.value || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    list.value = page.list
    total.value = page.total
    // 弹窗打开时同步详情
    if (detailOpen.value && detail.value) {
      detail.value = await adminGetOrder(detail.value.id)
    }
  }, 10000)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
})
</script>

<template>
  <div class="space-y-4">
    <!-- 筛选 -->
    <div class="card flex flex-wrap items-center gap-3 p-4">
      <select
        v-model="filterStatus"
        class="rounded-pill border border-cream-300 bg-white px-3 py-2 text-sm text-cocoa-600 outline-none"
        @change="pageNum = 1; load()"
      >
        <option v-for="option in STATUS_OPTIONS" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
      <input
        v-model="filterDate"
        type="date"
        class="rounded-pill border border-cream-300 bg-white px-3 py-2 text-sm text-cocoa-600 outline-none"
        @change="pageNum = 1; load()"
      />
      <button
        v-if="filterStatus || filterDate"
        type="button"
        class="text-sm text-cocoa-400"
        @click="filterStatus = ''; filterDate = ''; pageNum = 1; load()"
      >
        清除筛选
      </button>
    </div>

    <!-- 表格 -->
    <div class="card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[860px] text-sm">
          <thead>
            <tr class="bg-cream-100 text-left text-xs text-cocoa-400">
              <th class="px-4 py-3 font-normal">订单号</th>
              <th class="px-4 py-3 font-normal">宝贝</th>
              <th class="px-4 py-3 font-normal">菜品</th>
              <th class="px-4 py-3 font-normal">合计</th>
              <th class="px-4 py-3 font-normal">状态</th>
              <th class="px-4 py-3 font-normal">下单时间</th>
              <th class="px-4 py-3 text-right font-normal">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-cream-200">
            <tr
              v-for="order in list"
              :key="order.id"
              :class="order.status === 'PENDING' ? 'bg-honey-50' : 'hover:bg-cream-50'"
            >
              <td class="px-4 py-3 font-mono text-xs text-cocoa-500">{{ order.orderNo }}</td>
              <td class="px-4 py-3 text-cocoa-600">{{ order.nickname }}</td>
              <td class="max-w-[220px] px-4 py-3">
                <span class="truncate text-cocoa-600">
                  {{ (order.items || []).map((item) => item.dishName).join('、') }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span class="flex items-center gap-0.5 text-primary-500">
                  <Icon icon="ph:heart-fill" width="13" />{{ order.totalLove }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span class="rounded-pill px-2.5 py-0.5 text-[11px]" :class="statusClass(order.status)">
                  {{ order.statusLabel }}
                </span>
              </td>
              <td class="px-4 py-3 text-xs text-cocoa-400">{{ order.createTime }}</td>
              <td class="px-4 py-3 text-right">
                <button
                  type="button"
                  class="rounded-pill bg-primary-400 px-4 py-1.5 text-xs text-white"
                  @click="openDetail(order.id)"
                >
                  {{ ['PENDING', 'ACCEPTED', 'COOKING'].includes(order.status) ? '处理' : '查看' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-cream-200 p-3 text-sm text-cocoa-500">
        <span>共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button type="button" class="rounded-lg px-3 py-1.5 disabled:opacity-40" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">
            <Icon icon="ph:caret-left" />
          </button>
          <span>{{ pageNum }} / {{ totalPages() }}</span>
          <button type="button" class="rounded-lg px-3 py-1.5 disabled:opacity-40" :disabled="pageNum >= totalPages()" @click="goPage(pageNum + 1)">
            <Icon icon="ph:caret-right" />
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <Teleport to="body">
      <transition name="order-dialog">
        <div
          v-if="detailOpen && detail"
          class="fixed inset-0 z-50 flex items-center justify-center bg-cocoa-900/40 p-4"
          @click.self="detailOpen = false"
        >
          <div class="flex max-h-[92vh] w-full max-w-xl flex-col overflow-hidden rounded-2xl bg-white shadow-warm">
            <div class="flex items-center justify-between border-b border-cream-200 px-5 py-4">
              <div>
                <p class="text-base font-bold text-cocoa-700">订单 {{ detail.orderNo }}</p>
                <p class="text-xs text-cocoa-400">{{ detail.createTime }}</p>
              </div>
              <div class="flex items-center gap-3">
                <span class="rounded-pill px-2.5 py-0.5 text-[11px]" :class="statusClass(detail.status)">
                  {{ detail.statusLabel }}
                </span>
                <button type="button" aria-label="关闭" class="text-cocoa-300" @click="detailOpen = false">
                  <Icon icon="ph:x-circle" width="24" />
                </button>
              </div>
            </div>

            <div class="flex-1 overflow-y-auto p-5">
              <!-- 明细 -->
              <div class="space-y-3">
                <div v-for="item in detail.items" :key="item.dishId + item.taste + item.remark" class="flex items-center gap-3">
                  <img
                    :src="item.dishImage"
                    :alt="item.dishName"
                    class="h-12 w-14 flex-none rounded-lg bg-cream-200 object-cover"
                  />
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-medium text-cocoa-700">{{ item.dishName }}</p>
                    <p class="truncate text-xs text-cocoa-400">
                      {{ [item.taste, item.remark].filter(Boolean).join(' · ') || '—' }}
                    </p>
                  </div>
                  <span class="text-xs text-cocoa-400">×{{ item.quantity }}</span>
                </div>
              </div>

              <!-- 备注信息 -->
              <div class="mt-4 space-y-2 rounded-xl bg-cream-100 p-3 text-sm">
                <p v-if="detail.remark" class="text-cocoa-600">
                  <Icon icon="ph:chat-circle" class="mr-1 inline text-primary-400" />
                  订单备注：{{ detail.remark }}
                </p>
                <p v-if="detail.expectTime" class="text-cocoa-600">
                  <Icon icon="ph:clock" class="mr-1 inline text-primary-400" />
                  期望用餐：{{ detail.expectTime }}
                </p>
                <p v-if="detail.rejectReason" class="text-berry-500">
                  <Icon icon="ph:warning" class="mr-1 inline" />
                  拒绝理由：{{ detail.rejectReason }}
                </p>
                <p v-if="!detail.remark && !detail.expectTime && !detail.rejectReason" class="text-xs text-cocoa-300">
                  无额外备注
                </p>
              </div>

              <!-- 拒绝理由输入 -->
              <div v-if="rejectMode" class="mt-4">
                <p class="mb-2 text-sm font-medium text-cocoa-700">拒绝理由（会展示给宝贝）</p>
                <textarea
                  v-model="rejectReason"
                  rows="2"
                  maxlength="100"
                  class="input resize-none"
                  placeholder="如：今天食材没啦，明天做给你吃好不好"
                />
              </div>
            </div>

            <!-- 状态操作 -->
            <div class="flex gap-3 border-t border-cream-200 px-5 py-3">
              <template v-if="primaryAction">
                <button
                  v-if="detail.status === 'PENDING'"
                  type="button"
                  class="btn-ghost flex-1 py-2.5 text-berry-500"
                  @click="rejectMode = !rejectMode"
                >
                  拒绝订单
                </button>
                <button
                  v-if="rejectMode"
                  type="button"
                  class="btn-berry flex-1 py-2.5"
                  :disabled="busy"
                  @click="doReject"
                >
                  确认拒绝
                </button>
                <button
                  type="button"
                  class="btn-primary flex-1 py-2.5"
                  :disabled="busy || rejectMode"
                  @click="doPrimary"
                >
                  {{ primaryAction.label }}
                </button>
              </template>
              <p v-else class="flex-1 py-2 text-center text-xs text-cocoa-300">
                订单已结束，无待处理操作
              </p>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
.order-dialog-enter-active,
.order-dialog-leave-active {
  transition: opacity 0.18s ease;
}
.order-dialog-enter-from,
.order-dialog-leave-to {
  opacity: 0;
}
</style>
