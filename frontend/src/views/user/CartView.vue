<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import EmptyState from '@/components/EmptyState.vue'
import QuantityStepper from '@/components/QuantityStepper.vue'
import { useCartStore } from '@/stores/cart'
import { toast } from '@/utils/toast'

const router = useRouter()
const cartStore = useCartStore()
const busy = ref(false)

const validLines = computed(() => cartStore.lines.filter((line) => line.available))

async function onQtyChange(id: number, quantity: number, line: { taste: string; remark: string }) {
  if (quantity === 0) {
    await cartStore.removeLine(id)
    return
  }
  await cartStore.changeLine(id, { quantity, taste: line.taste, remark: line.remark })
}

async function saveRemark(id: number, line: { quantity: number; taste: string; remark: string }) {
  await cartStore.changeLine(id, {
    quantity: line.quantity,
    taste: line.taste,
    remark: line.remark,
  })
  toast('备注已保存')
}

async function clearCart() {
  busy.value = true
  try {
    await cartStore.clear()
    toast('购物车已清空')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100">
    <!-- 头部 -->
    <header class="pb-safe-2 sticky top-0 z-30 flex items-center justify-between border-b border-cream-300 bg-cream-100/95 px-4 py-3 pt-safe backdrop-blur">
      <button type="button" aria-label="返回" class="text-cocoa-500" @click="router.back()">
        <Icon icon="ph:caret-left" width="28" />
      </button>
      <h1 class="text-base font-bold text-cocoa-700">购物车</h1>
      <button
        v-if="cartStore.lines.length"
        type="button"
        class="flex items-center gap-1 text-sm text-cocoa-500"
        :disabled="busy"
        @click="clearCart"
      >
        <Icon icon="ph:trash" /> 清空
      </button>
      <span v-else class="w-10" />
    </header>

    <!-- 空状态 -->
    <div v-if="cartStore.lines.length === 0" class="flex flex-1 flex-col">
      <EmptyState icon="ph:shopping-cart-simple" text="购物车空空的" hint="去挑几道大厨的拿手菜吧">
        <button type="button" class="btn-primary mt-2" @click="router.replace('/home')">
          <Icon icon="ph:bowl-food" /> 去逛逛
        </button>
      </EmptyState>
    </div>

    <template v-else>
      <div class="flex-1 space-y-3 px-4 py-4 pb-32">
        <!-- 不可购买提示 -->
        <div
          v-if="cartStore.hasUnavailable"
          class="flex items-center gap-2 rounded-2xl bg-berry-50 p-3 text-xs text-berry-500"
        >
          <Icon icon="ph:warning" width="18" />
          有菜品已下架，不会计入订单，可手动移除
        </div>

        <div
          v-for="line in cartStore.lines"
          :key="line.id"
          class="rounded-2xl bg-white p-3"
          :class="{ 'opacity-60': !line.available }"
        >
          <div class="flex items-center gap-3">
            <img
              :src="line.image"
              :alt="line.name"
              class="h-16 w-20 flex-none rounded-xl bg-cream-200 object-cover"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-cocoa-700">{{ line.name }}</p>
              <p v-if="line.taste" class="mt-0.5 text-xs text-cocoa-500">口味：{{ line.taste }}</p>
              <p v-if="!line.available" class="mt-0.5 text-xs text-berry-500">已下架</p>
              <p v-else class="mt-0.5 flex items-center gap-0.5 text-xs text-primary-500">
                <Icon icon="ph:heart-fill" width="12" />
                {{ line.loveValue }} × {{ line.quantity }} = {{ line.lineLove }}
              </p>
            </div>
            <QuantityStepper
              :model-value="line.quantity"
              :min="0"
              :disabled="!line.available"
              @update:model-value="onQtyChange(line.id, $event, line)"
            />
          </div>

          <!-- 单品备注（可编辑） -->
          <div v-if="line.available" class="mt-2 flex items-center gap-2">
            <Icon icon="ph:pencil-line" class="flex-none text-cocoa-300" width="16" />
            <input
              :value="line.remark"
              class="w-full rounded-pill bg-cream-100 px-3 py-1.5 text-xs text-cocoa-600 outline-none placeholder:text-cocoa-300 focus:bg-white focus:ring-2 focus:ring-primary-200"
              placeholder="给这道菜写句备注/悄悄话…"
              maxlength="100"
              @input="line.remark = ($event.target as HTMLInputElement).value"
              @blur="saveRemark(line.id, line)"
            />
          </div>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="pb-safe fixed bottom-0 left-0 right-0 border-t border-cream-300 bg-white px-5 py-3">
        <div class="flex items-center gap-4">
          <div class="flex-1">
            <p class="text-xs text-cocoa-500">合计爱心值</p>
            <p class="flex items-center gap-1 text-xl font-bold text-primary-500">
              <Icon icon="ph:heart-fill" />
              {{ cartStore.totalLove }}
            </p>
          </div>
          <button
            type="button"
            class="btn-primary px-10 py-3"
            :disabled="validLines.length === 0"
            @click="router.push('/order-confirm')"
          >
            去结算
          </button>
        </div>
      </div>
    </template>
  </div>
</template>
