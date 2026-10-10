<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/toast'

const router = useRouter()
const userStore = useUserStore()

const entries = [
  { label: '我的收藏', desc: '爱吃的菜都在这里', icon: 'ph:star', to: '/me/favorites' },
  { label: '吃饭日历', desc: '回顾一起吃过的饭', icon: 'ph:calendar-heart', to: '/me/calendar' },
  { label: '修改密码', desc: '换个只有我们知道的密码', icon: 'ph:lock-key', to: '/me/password' },
]

async function handleLogout() {
  await userStore.logout()
  toast('已退出登录，记得想我~')
  router.replace('/login')
}
</script>

<template>
  <div class="pb-24">
    <header class="bg-gradient-to-b from-berry-100 to-cream-100 px-5 pb-8 pt-8 pt-safe">
      <div class="flex items-center gap-4">
        <div class="flex h-[4.5rem] w-[4.5rem] items-center justify-center overflow-hidden rounded-full bg-white shadow-card">
          <img
            v-if="userStore.user?.avatar"
            :src="userStore.user.avatar"
            :alt="userStore.nickname"
            class="h-full w-full object-cover"
          />
          <Icon v-else icon="ph:user-circle" class="text-primary-300" width="48" />
        </div>
        <div>
          <div class="flex items-center gap-2">
            <p class="text-xl font-bold text-cocoa-700">{{ userStore.nickname }}</p>
            <span
              class="rounded-pill px-2 py-0.5 text-[10px]"
              :class="userStore.isAdmin ? 'bg-honey-100 text-honey-600' : 'bg-berry-100 text-berry-500'"
            >
              {{ userStore.isAdmin ? '大厨' : '宝贝' }}
            </span>
          </div>
          <p class="mt-0.5 text-xs text-cocoa-400">账号：{{ userStore.user?.username }}</p>
        </div>
      </div>
    </header>

    <div class="px-4">
      <!-- 大厨入口 -->
      <button
        v-if="userStore.isAdmin"
        type="button"
        class="card mb-3 flex w-full items-center gap-3 p-4 text-left"
        @click="router.push('/admin')"
      >
        <div class="flex h-10 w-10 items-center justify-center rounded-full bg-honey-100 text-honey-500">
          <Icon icon="ph:chef-hat" width="22" />
        </div>
        <div class="flex-1">
          <p class="text-sm font-medium text-cocoa-700">大厨工作台</p>
          <p class="text-xs text-cocoa-400">管理菜品、处理订单</p>
        </div>
        <Icon icon="ph:caret-right" class="text-cocoa-300" />
      </button>

      <!-- 功能入口 -->
      <div class="card divide-y divide-cream-200">
        <button
          v-for="entry in entries"
          :key="entry.label"
          type="button"
          class="flex w-full items-center gap-3 p-4 text-left"
          @click="router.push(entry.to)"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary-100 text-primary-500">
            <Icon :icon="entry.icon" width="22" />
          </div>
          <div class="flex-1">
            <p class="text-sm font-medium text-cocoa-700">{{ entry.label }}</p>
            <p class="text-xs text-cocoa-400">{{ entry.desc }}</p>
          </div>
          <Icon icon="ph:caret-right" class="text-cocoa-300" />
        </button>
      </div>

      <button type="button" class="btn-ghost mt-6 w-full py-3 text-berry-500" @click="handleLogout">
        <Icon icon="ph:sign-out" /> 退出登录
      </button>

      <p class="mt-8 text-center text-xs text-cocoa-300">
        一屋两人 · 三餐四季
      </p>
    </div>
  </div>
</template>
