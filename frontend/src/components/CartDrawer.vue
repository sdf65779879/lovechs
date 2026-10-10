<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import QuantityStepper from '@/components/QuantityStepper.vue'
import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'
import { toast } from '@/utils/toast'

const router = useRouter()
const cartStore = useCartStore()
const uiStore = useUiStore()
const busy = ref(false)

const validLines = computed(() => cartStore.lines.filter((line) => line.available))

async function onQtyChange(id: number, quantity: number, line: { taste: string; remark: string }) {
  if (quantity === 0) {
    await cartStore.removeLine(id)
    return
  }
  await cartStore.changeLine(id, {
    quantity,
    taste: line.taste,
    remark: line.remark,
  })
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

function goCheckout() {
  if (validLines.value.length === 0) return
  uiStore.closeCartDrawer()
  router.push('/order-confirm')
}
</script>

<template>
  <Teleport to="body">
    <transition name="drawer-fade">
      <div
        v-if="uiStore.cartDrawerOpen"
        class="fixed inset-0 z-50 bg-cocoa-900/40"
        @click="uiStore.closeCartDrawer()"
      />
    </transition>

    <transition name="drawer-slide">
      <div
        v-if="uiStore.cartDrawerOpen"
        class="pb-safe fixed bottom-0 left-0 right-0 z-50 mx-auto flex max-h-[78vh] w-full max-w-lg flex-col rounded-t-3xl bg-cream-100 shadow-warm"
      >
        <!-- 头部 -->
        <div class="flex items-center justify-between px-5 pb-3 pt-4">
          <div class="flex items-center gap-2 text-lg font-bold text-cocoa-700">
            <Icon icon="ph:shopping-cart" class="text-primary-400" />
            购物车
            <span class="text-sm font-normal text-cocoa-300">
              共 {{ cartStore.count }} 件
            </span>
          </div>
          <div class="flex items-center gap-3">
            <button
              v-if="cartStore.lines.length"
              type="button"
              class="flex items-center gap-1 text-sm text-cocoa-500"
              :disabled="busy"
              @click="clearCart"
            >
              <Icon icon="ph:trash" /> 清空
            </button>
            <button
              type="button"
              aria-label="关闭"
              class="text-cocoa-300"
              @click="uiStore.closeCartDrawer()"
            >
              <Icon icon="ph:x-circle" width="24" />
            </button>
          </div>
        </div>

        <!-- 列表 -->
        <div class="flex-1 overflow-y-auto px-4">
          <div
            v-if="cartStore.lines.length === 0"
            class="flex flex-col items-center gap-3 py-12 text-cocoa-300"
          >
            <Icon icon="ph:shopping-cart-simple" width="56" />
            <p class="text-sm">购物车空空的，去挑点好吃的吧~</p>
          </div>

          <div
            v-for="line in cartStore.lines"
            :key="line.id"
            class="mb-2 flex items-center gap-3 rounded-2xl bg-white p-3"
            :class="{ 'opacity-60': !line.available }"
          >
            <img
              :src="line.image"
              :alt="line.name"
              class="h-14 w-16 flex-none rounded-xl bg-cream-200 object-cover"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-medium text-cocoa-700">{{ line.name }}</p>
              <p v-if="line.taste || line.remark" class="mt-0.5 truncate text-xs text-cocoa-500">
                {{ [line.taste, line.remark].filter(Boolean).join(' · ') }}
              </p>
              <p v-if="!line.available" class="mt-0.5 text-xs text-berry-500">已下架，无法下单</p>
              <p v-else class="mt-0.5 flex items-center gap-0.5 text-xs text-primary-500">
                <Icon icon="ph:heart-fill" width="12" />
                {{ line.loveValue }} × {{ line.quantity }} = {{ line.lineLove }}
              </p>
            </div>
            <QuantityStepper
              :model-value="line.quantity"
              :min="0"
              small
              @update:model-value="onQtyChange(line.id, $event, line)"
            />
          </div>
        </div>

        <!-- 底部结算 -->
        <div class="border-t border-cream-300 px-5 py-3">
          <div class="mb-2 flex items-center justify-between text-sm">
            <span class="text-cocoa-500">合计爱心值</span>
            <span class="flex items-center gap-1 text-lg font-bold text-primary-500">
              <Icon icon="ph:heart-fill" />
              {{ cartStore.totalLove }}
            </span>
          </div>
          <button
            type="button"
            class="btn-primary w-full py-3"
            :disabled="validLines.length === 0"
            @click="goCheckout"
          >
            去 结 算
          </button>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.2s ease;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.25s ease;
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateY(100%);
}
</style>
