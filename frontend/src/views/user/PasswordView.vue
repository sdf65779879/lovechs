<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { changePassword } from '@/api/auth'
import { toast } from '@/utils/toast'

const router = useRouter()

const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const submitting = ref(false)

async function handleSubmit() {
  if (!oldPassword.value || !newPassword.value) {
    toast('请填写完整')
    return
  }
  if (newPassword.value.length < 6) {
    toast('新密码至少 6 位')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast('两次输入的新密码不一致')
    return
  }
  submitting.value = true
  try {
    await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    toast('密码修改成功')
    router.back()
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
      <h1 class="text-base font-bold text-cocoa-700">修改密码</h1>
      <span class="w-7" />
    </header>

    <form class="space-y-4 p-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="mb-1.5 block text-sm text-cocoa-700">当前密码</span>
        <input v-model="oldPassword" type="password" class="input" placeholder="请输入当前密码" />
      </label>
      <label class="block">
        <span class="mb-1.5 block text-sm text-cocoa-700">新密码</span>
        <input v-model="newPassword" type="password" class="input" placeholder="至少 6 位" />
      </label>
      <label class="block">
        <span class="mb-1.5 block text-sm text-cocoa-700">确认新密码</span>
        <input v-model="confirmPassword" type="password" class="input" placeholder="再输入一次" />
      </label>

      <button type="submit" class="btn-primary w-full py-3" :disabled="submitting">
        <Icon v-if="submitting" icon="svg-spinners:ring-resize" />
        {{ submitting ? '保存中…' : '保存修改' }}
      </button>
    </form>
  </div>
</template>
