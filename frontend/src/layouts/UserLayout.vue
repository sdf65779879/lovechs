<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

import CartDrawer from '@/components/CartDrawer.vue'
import CartFloating from '@/components/CartFloating.vue'
import DishDetailSheet from '@/components/DishDetailSheet.vue'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const cartStore = useCartStore()
const userStore = useUserStore()

onMounted(async () => {
  try {
    await cartStore.fetchCart()
  } catch {
    // 忽略：未登录或网络问题由拦截器处理
  }
})

const tabs = [
  { name: 'home', label: '菜单', icon: 'ph:house', to: '/home' },
  { name: 'orders', label: '订单', icon: 'ph:receipt', to: '/orders' },
  { name: 'me', label: '我的', icon: 'ph:user-circle', to: '/me' },
]

const hiddenTabRoutes = ['cart', 'order-confirm', 'order-detail', 'my-password', 'my-calendar']
const showTabBar = computed(() => !hiddenTabRoutes.includes(route.name as string))
const showCartFloat = computed(() => ['home', 'my-favorites'].includes(route.name as string))

function isActive(name: string) {
  if (name === 'orders') {
    return route.name === 'orders' || route.name === 'order-detail'
  }
  return route.name === name
}
</script>

<template>
  <div class="min-h-full bg-cream-100">
    <RouterView />

    <!-- 悬浮购物车 -->
    <CartFloating v-if="showCartFloat && userStore.isLogin" />

    <!-- 底部 Tab 栏（适配安全区） -->
    <nav
      v-if="showTabBar"
      class="pb-safe fixed bottom-0 left-0 right-0 z-40 border-t border-cream-300 bg-white/95 backdrop-blur"
    >
      <div class="mx-auto flex max-w-lg">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.name"
          :to="tab.to"
          class="flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] transition"
          :class="isActive(tab.name) ? 'text-primary-400' : 'text-cocoa-300'"
        >
          <Icon :icon="tab.icon" width="24" />
          {{ tab.label }}
        </RouterLink>
      </div>
    </nav>

    <!-- 全局抽屉与弹层 -->
    <CartDrawer />
    <DishDetailSheet />
  </div>
</template>
