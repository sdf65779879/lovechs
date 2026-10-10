<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useCartStore } from '@/stores/cart'
import { submitOrder } from '@/api/order'
import { toast } from '@/utils/toast'

const router = useRouter()
const cartStore = useCartStore()

const remark = ref('')
const expectMode = ref<'now' | 'time'>('now')
const expectTime = ref('')
const submitting = ref(false)

const validLines = computed(() => cartStore.lines.filter((line) => line.available))

onMounted(async () => {
  if (!cartStore.loaded) {
    await cartStore.fetchCart()
  }
  if (validLines.value.length === 0) {
    toast('购物车是空的，先去点菜吧')
    router.replace('/home')
  }
})

/** datetime-local 值转为后端友好的 yyyy-MM-dd HH:mm */
function formatExpectTime(value: string): string {
  return value.replace('T', ' ')
}

async function handleSubmit() {
  if (submitting.value) return
  if (expectMode.value === 'time' && !expectTime.value) {
    toast('请选择期望用餐时间')
    return
  }
  submitting.value = true
  // 幂等键：每次进入页面生成一次，重复点击也只会下一单
  const idempotencyKey =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
  try {
    const order = await submitOrder(
      {
        remark: remark.value.trim(),
        expectTime:
          expectMode.value === 'time' ? formatExpectTime(expectTime.value) : undefined,
      },
      idempotencyKey,
    )
    await cartStore.fetchCart()
    toast('下单成功，大厨马上就来~')
    router.replace(`/orders/${order.id}`)
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100">
    <header class="pb-safe-2 sticky top-0 z-30 flex items-center justify-between border-b border-cream-300 bg-cream-100/95 px-4 py-3 pt-safe backdrop-blur">
      <button type="button" aria-label="返回" class="text-cocoa-500" @click="router.back()">
        <Icon icon="ph:caret-left" width="28" />
      </button>
      <h1 class="text-base font-bold text-cocoa-700">确认订单</h1>
      <span class="w-7" />
    </header>

    <div class="flex-1 space-y-3 px-4 py-4 pb-32">
      <!-- 致大厨 -->
      <div class="flex items-center gap-2 rounded-2xl bg-primary-100 p-3 text-sm text-cocoa-600">
        <Icon icon="ph:chef-hat" class="text-primary-400" width="20" />
        这一单，做给最爱的大厨看
      </div>

      <!-- 菜品清单 -->
      <div class="card p-4">
        <p class="mb-3 text-sm font-bold text-cocoa-700">菜品清单</p>
        <div class="space-y-3">
          <div v-for="line in validLines" :key="line.id" class="flex items-center gap-3">
            <img
              :src="line.image"
              :alt="line.name"
              class="h-12 w-14 flex-none rounded-lg bg-cream-200 object-cover"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm text-cocoa-700">{{ line.name }}</p>
              <p class="truncate text-xs text-cocoa-400">
                {{ [line.taste, line.remark].filter(Boolean).join(' · ') || '—' }}
              </p>
            </div>
            <span class="text-xs text-cocoa-400">×{{ line.quantity }}</span>
            <span class="flex w-12 flex-none items-center justify-end gap-0.5 text-sm text-primary-500">
              <Icon icon="ph:heart-fill" width="12" />
              {{ line.lineLove }}
            </span>
          </div>
        </div>
        <div class="mt-3 flex items-center justify-between border-t border-dashed border-cream-300 pt-3">
          <span class="text-sm text-cocoa-500">合计爱心值</span>
          <span class="flex items-center gap-1 text-lg font-bold text-primary-500">
            <Icon icon="ph:heart-fill" />
            {{ cartStore.totalLove }}
          </span>
        </div>
      </div>

      <!-- 期望用餐时间 -->
      <div class="card p-4">
        <p class="mb-3 text-sm font-bold text-cocoa-700">期望用餐时间</p>
        <div class="flex gap-2">
          <button
            type="button"
            class="flex flex-1 items-center justify-center gap-1.5 rounded-pill border px-3 py-2 text-sm transition"
            :class="
              expectMode === 'now'
                ? 'border-primary-400 bg-primary-400 text-white'
                : 'border-cream-300 bg-white text-cocoa-500'
            "
            @click="expectMode = 'now'"
          >
            <Icon icon="ph:lightning" /> 尽快
          </button>
          <button
            type="button"
            class="flex flex-1 items-center justify-center gap-1.5 rounded-pill border px-3 py-2 text-sm transition"
            :class="
              expectMode === 'time'
                ? 'border-primary-400 bg-primary-400 text-white'
                : 'border-cream-300 bg-white text-cocoa-500'
            "
            @click="expectMode = 'time'"
          >
            <Icon icon="ph:clock" /> 指定时间
          </button>
        </div>
        <input
          v-if="expectMode === 'time'"
          v-model="expectTime"
          type="datetime-local"
          class="input mt-3"
        />
      </div>

      <!-- 订单备注 -->
      <div class="card p-4">
        <p class="mb-2 text-sm font-bold text-cocoa-700">
          订单备注 <span class="font-normal text-cocoa-300">（口味 / 忌口 / 撒娇留言）</span>
        </p>
        <textarea
          v-model="remark"
          rows="3"
          maxlength="200"
          class="input resize-none"
          placeholder="比如：今天想吃软一点的米饭……或者：大厨辛苦啦，爱你！"
        />
        <p class="mt-1 text-right text-[10px] text-cocoa-300">{{ remark.length }}/200</p>
      </div>
    </div>

    <!-- 底部提交 -->
    <div class="pb-safe fixed bottom-0 left-0 right-0 border-t border-cream-300 bg-white px-5 py-3">
      <button
        type="button"
        class="btn-primary w-full py-3"
        :disabled="submitting"
        @click="handleSubmit"
      >
        <Icon v-if="submitting" icon="svg-spinners:ring-resize" />
        {{ submitting ? '正在下单…' : `提交订单 · ${cartStore.totalLove} 爱心` }}
      </button>
    </div>
  </div>
</template>
