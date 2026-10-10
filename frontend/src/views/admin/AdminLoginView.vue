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
const loading = ref(false)

async function handleLogin() {
  if (!username.value.trim() || !password.value) return
  loading.value = true
  try {
    await userStore.login(username.value.trim(), password.value, true)
    // 非管理员即便登录成功也不允许进入工作台
    if (!userStore.isAdmin) {
      await userStore.logout()
      return
    }
    const redirect = (route.query.redirect as string) || '/admin/dashboard'
    router.replace(redirect)
  } catch {
    // 错误提示由拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full items-center justify-center bg-gradient-to-br from-primary-100 via-cream-100 to-honey-100 p-6">
    <div class="w-full max-w-sm">
      <div class="mb-8 text-center">
        <div
          class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary-400 shadow-warm"
        >
          <Icon icon="ph:chef-hat" width="36" class="text-white" />
        </div>
        <h1 class="text-2xl font-bold text-cocoa-700">大厨工作台</h1>
        <p class="mt-1 text-sm text-cocoa-500">登录后为宝贝做饭~</p>
      </div>

      <form class="card space-y-4 p-6" @submit.prevent="handleLogin">
        <label class="block">
          <span class="mb-1.5 block text-sm text-cocoa-700">管理员账号</span>
          <div class="relative">
            <Icon
              icon="ph:user"
              class="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-cocoa-300"
              width="18"
            />
            <input
              v-model="username"
              class="input pl-10"
              placeholder="请输入管理员账号"
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

        <button type="submit" class="btn-primary w-full py-3" :disabled="loading">
          <Icon v-if="loading" icon="svg-spinners:ring-resize" />
          {{ loading ? '登录中…' : '进 入 工 作 台' }}
        </button>
      </form>

      <p class="mt-6 text-center text-xs text-cocoa-300">
        <RouterLink to="/login" class="hover:text-primary-400">宝贝点这里回用户端</RouterLink>
      </p>
    </div>
  </div>
</template>
