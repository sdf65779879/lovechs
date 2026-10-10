<script setup lang="ts">
import { Icon } from '@iconify/vue'

import { useCartStore } from '@/stores/cart'
import { useUiStore } from '@/stores/ui'

const cartStore = useCartStore()
const uiStore = useUiStore()
</script>

<template>
  <button
    type="button"
    aria-label="购物车"
    class="pb-safe fixed bottom-[5.5rem] right-4 z-40 flex h-14 w-14 items-center justify-center rounded-full bg-primary-400 text-white shadow-warm transition active:scale-90"
    @click="uiStore.openCartDrawer()"
  >
    <Icon icon="ph:shopping-cart" width="26" />
    <span
      v-if="cartStore.count > 0"
      class="absolute -right-1 -top-1 flex h-5 min-w-[20px] items-center justify-center rounded-full border-2 border-white bg-berry-400 px-1 text-[11px] font-medium"
    >
      {{ cartStore.count > 99 ? '99+' : cartStore.count }}
    </span>
  </button>
</template>
