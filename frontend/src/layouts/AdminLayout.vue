<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { adminPageOrders } from '@/api/admin'
import type { Order } from '@/api/types'
import { useUserStore } from '@/stores/user'
import { playDing } from '@/utils/notify-sound'
import { startTitleFlash, stopTitleFlash } from '@/utils/title-flash'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { name: 'admin-dashboard', label: '数据看板', icon: 'ph:squares-four' },
  { name: 'admin-orders', label: '订单管理', icon: 'ph:receipt' },
  { name: 'admin-dishes', label: '菜品管理', icon: 'ph:bowl-food' },
  { name: 'admin-categories', label: '分类管理', icon: 'ph:list-bullets' },
  { name: 'admin-users', label: '用户管理', icon: 'ph:users' },
  { name: 'admin-settings', label: '系统设置', icon: 'ph:gear-six' },
]

const pendingCount = ref(0)
const alerting = ref(false)
let knownPendingIds = new Set<number>()
let pollTimer: number | undefined
let baseline = false

/** 轮询待接单订单：发现新单则标题闪烁 + 提示音 */
async function pollPending() {
  const page = await adminPageOrders({ status: 'PENDING', pageNum: 1, pageSize: 50 })
  pendingCount.value = page.total
  const currentIds = new Set(page.list.map((o: Order) => o.id))
  if (!baseline) {
    // 首次只建立基线，登录前已存在的订单不重复提醒
    knownPendingIds = currentIds
    baseline = true
    if (page.total > 0) {
      alerting.value = true
      startTitleFlash()
    }
    return
  }
  const fresh = page.list.filter((o: Order) => !knownPendingIds.has(o.id))
  knownPendingIds = currentIds
  if (fresh.length > 0) {
    alerting.value = true
    startTitleFlash()
    playDing()
  }
  if (page.total === 0) {
    alerting.value = false
    stopTitleFlash()
  }
}

/** 点击铃铛/订单菜单：确认提醒并跳转 */
function acknowledge() {
  alerting.value = false
  stopTitleFlash()
  router.push('/admin/orders')
}

async function handleLogout() {
  stopTitleFlash()
  await userStore.logout()
  router.replace('/admin/login')
}

onMounted(() => {
  pollPending()
  pollTimer = window.setInterval(pollPending, 10000)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
  stopTitleFlash()
})

const currentMenu = computed(() => route.name as string)
</script>

<template>
  <div class="min-h-full bg-cream-100">
    <!-- PC 侧边栏 -->
    <aside
      class="fixed inset-y-0 left-0 z-40 hidden w-56 flex-col border-r border-cream-300 bg-white md:flex"
    >
      <div class="flex items-center gap-2 px-5 py-5">
        <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-primary-400">
          <Icon icon="ph:chef-hat" class="text-white" width="22" />
        </div>
        <div>
          <p class="text-sm font-bold text-cocoa-700">大厨工作台</p>
          <p class="text-[10px] text-cocoa-300">情侣点餐系统</p>
        </div>
      </div>

      <nav class="flex-1 space-y-1 px-3">
        <RouterLink
          v-for="menu in menus"
          :key="menu.name"
          :to="{ name: menu.name }"
          class="flex items-center gap-2.5 rounded-xl px-3 py-2.5 text-sm transition"
          :class="
            currentMenu === menu.name
              ? 'bg-primary-100 font-medium text-primary-500'
              : 'text-cocoa-500 hover:bg-cream-100'
          "
        >
          <Icon :icon="menu.icon" width="20" />
          {{ menu.label }}
          <span
            v-if="menu.name === 'admin-orders' && pendingCount > 0"
            class="ml-auto flex h-5 min-w-[20px] items-center justify-center rounded-full bg-berry-400 px-1.5 text-[10px] text-white"
          >
            {{ pendingCount }}
          </span>
        </RouterLink>
      </nav>

      <div class="border-t border-cream-200 p-3">
        <div class="flex items-center gap-2 px-2 py-1.5">
          <Icon icon="ph:user-circle" class="text-cocoa-400" width="22" />
          <span class="flex-1 truncate text-sm text-cocoa-600">{{ userStore.nickname }}</span>
        </div>
        <button
          type="button"
          class="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-sm text-cocoa-500 hover:bg-cream-100"
          @click="handleLogout"
        >
          <Icon icon="ph:sign-out" width="20" /> 退出登录
        </button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <div class="flex min-h-full flex-col md:pl-56">
      <!-- 顶栏 -->
      <header
        class="sticky top-0 z-30 flex items-center gap-3 border-b border-cream-300 bg-white px-4 py-3"
      >
        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-400 md:hidden">
          <Icon icon="ph:chef-hat" class="text-white" width="20" />
        </div>
        <h1 class="flex-1 text-base font-bold text-cocoa-700">
          {{ route.meta.title }}
        </h1>

        <button
          type="button"
          aria-label="新订单提醒"
          class="relative flex h-9 w-9 items-center justify-center rounded-full transition"
          :class="alerting ? 'animate-soft-bounce bg-berry-50 text-berry-500' : 'text-cocoa-400'"
          @click="acknowledge"
        >
          <Icon icon="ph:bell" width="22" />
          <span
            v-if="pendingCount > 0"
            class="absolute -right-0.5 -top-0.5 flex h-4 min-w-[16px] items-center justify-center rounded-full bg-berry-400 px-1 text-[9px] text-white"
          >
            {{ pendingCount }}
          </span>
        </button>

        <RouterLink
          to="/home"
          class="hidden items-center gap-1 rounded-pill bg-cream-100 px-3 py-1.5 text-xs text-cocoa-500 sm:flex"
        >
          <Icon icon="ph:house" width="14" /> 用户端
        </RouterLink>
      </header>

      <!-- 移动端横向菜单 -->
      <div class="no-scrollbar flex gap-2 overflow-x-auto border-b border-cream-300 bg-white px-4 py-2 md:hidden">
        <RouterLink
          v-for="menu in menus"
          :key="menu.name"
          :to="{ name: menu.name }"
          class="flex flex-none items-center gap-1 rounded-pill px-3 py-1.5 text-xs"
          :class="
            currentMenu === menu.name
              ? 'bg-primary-400 text-white'
              : 'bg-cream-100 text-cocoa-500'
          "
        >
          <Icon :icon="menu.icon" width="14" />
          {{ menu.label }}
        </RouterLink>
      </div>

      <main class="flex-1 p-4 md:p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
