<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const remember = ref(true)
const loading = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value) return
  loading.value = true
  try {
    await userStore.login(username.value.trim(), password.value, remember.value)
    const redirect = (route.query.redirect as string) || '/home'
    router.replace(redirect)
  } catch {
    // 错误提示已在响应拦截统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full flex-col bg-cream-100 px-7 pt-safe">
    <div class="flex flex-1 flex-col justify-center py-10">
      <!-- 品牌区 -->
      <div class="mb-10 text-center">
        <div
          class="mx-auto mb-4 flex h-20 w-20 animate-soft-bounce items-center justify-center rounded-3xl bg-primary-400 shadow-warm"
        >
          <Icon icon="ph:bowl-food" width="42" class="text-white" />
        </div>
        <h1 class="text-2xl font-bold text-cocoa-700">欢迎回家</h1>
        <p class="mt-2 text-sm text-cocoa-500">今天想吃点什么？大厨随时待命~</p>
      </div>

      <!-- 表单 -->
      <form class="space-y-4" @submit.prevent="handleLogin">
        <label class="block">
          <span class="mb-1.5 block text-sm text-cocoa-700">账号</span>
          <div class="relative">
            <Icon
              icon="ph:user"
              class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-cocoa-300"
              width="18"
            />
            <input
              v-model="username"
              class="input pl-10"
              placeholder="请输入账号"
              autocomplete="username"
            />
          </div>
        </label>

        <label class="block">
          <span class="mb-1.5 block text-sm text-cocoa-700">密码</span>
          <div class="relative">
            <Icon
              icon="ph:lock"
              class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-cocoa-300"
              width="18"
            />
            <input
              v-model="password"
              type="password"
              class="input pl-10"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </div>
        </label>

        <label class="flex cursor-pointer items-center gap-2 text-sm text-cocoa-500">
          <input v-model="remember" type="checkbox" class="h-4 w-4 accent-primary-400" />
          记住我（下次自动登录）
        </label>

        <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
          <Icon v-if="loading" icon="svg-spinners:ring-resize" />
          {{ loading ? '正在登录…' : '开 饭' }}
        </button>
      </form>

      <p class="mt-8 text-center text-xs text-cocoa-300">
        账号由大厨创建 · 有问题找大厨
      </p>
    </div>
  </div>
</template>
