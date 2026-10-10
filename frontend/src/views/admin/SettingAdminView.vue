<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'

import { adminListConfigs, adminUpdateConfigs } from '@/api/admin'
import { changePassword } from '@/api/auth'
import { toast } from '@/utils/toast'

const loading = ref(true)
const businessOpen = ref(true)
const notice = ref('')
const savingConfig = ref(false)

// 修改密码
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const savingPassword = ref(false)

async function load() {
  loading.value = true
  try {
    const configs = await adminListConfigs()
    const map = new Map(configs.map((item) => [item.configKey, item.configValue]))
    businessOpen.value = map.get('business_open') !== '0'
    notice.value = map.get('notice') ?? ''
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  savingConfig.value = true
  try {
    await adminUpdateConfigs({
      business_open: businessOpen.value ? '1' : '0',
      notice: notice.value.trim(),
    })
    toast('设置已保存')
  } finally {
    savingConfig.value = false
  }
}

async function savePassword() {
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
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    })
    toast('密码修改成功，请牢记')
    oldPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
  } finally {
    savingPassword.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="mx-auto max-w-2xl space-y-5">
    <!-- 营业设置 -->
    <div class="card p-5">
      <p class="mb-4 text-sm font-bold text-cocoa-700">营业设置</p>

      <div
        class="flex items-center justify-between rounded-xl p-4"
        :class="businessOpen ? 'bg-primary-50' : 'bg-cream-200'"
      >
        <div class="flex items-center gap-3">
          <Icon
            :icon="businessOpen ? 'ph:storefront' : 'ph:moon-stars'"
            class="text-primary-500"
            width="26"
          />
          <div>
            <p class="text-sm font-medium text-cocoa-700">
              {{ businessOpen ? '正在营业' : '暂停营业' }}
            </p>
            <p class="text-xs text-cocoa-400">
              {{ businessOpen ? '宝贝可以正常下单' : '用户端将展示「今日暂不营业」' }}
            </p>
          </div>
        </div>

        <button
          type="button"
          class="relative h-7 w-12 rounded-full transition"
          :class="businessOpen ? 'bg-primary-400' : 'bg-cream-300'"
          :aria-label="businessOpen ? '点击打烊' : '点击开业'"
          @click="businessOpen = !businessOpen"
        >
          <span
            class="absolute top-1 h-5 w-5 rounded-full bg-white shadow transition-all"
            :class="businessOpen ? 'left-[26px]' : 'left-1'"
          />
        </button>
      </div>

      <label class="mt-4 block">
        <span class="mb-1.5 block text-sm text-cocoa-700">
          首页公告
          <span class="text-xs font-normal text-cocoa-300">（留空则不展示）</span>
        </span>
        <textarea
          v-model="notice"
          rows="3"
          maxlength="100"
          class="input resize-none"
          placeholder="如：今晚有新菜！或者：今天大厨有点累，只做简单的~"
        />
      </label>

      <button
        type="button"
        class="btn-primary mt-4 w-full py-2.5"
        :disabled="savingConfig"
        @click="saveConfig"
      >
        {{ savingConfig ? '保存中…' : '保存营业设置' }}
      </button>
    </div>

    <!-- 修改管理员密码 -->
    <div class="card p-5">
      <p class="mb-4 text-sm font-bold text-cocoa-700">修改管理员密码</p>
      <label class="mb-3 block">
        <span class="mb-1 block text-sm text-cocoa-500">当前密码</span>
        <input v-model="oldPassword" type="password" class="input" placeholder="请输入当前密码" />
      </label>
      <label class="mb-3 block">
        <span class="mb-1 block text-sm text-cocoa-500">新密码</span>
        <input v-model="newPassword" type="password" class="input" placeholder="至少 6 位" />
      </label>
      <label class="mb-4 block">
        <span class="mb-1 block text-sm text-cocoa-500">确认新密码</span>
        <input v-model="confirmPassword" type="password" class="input" placeholder="再输入一次" />
      </label>
      <button
        type="button"
        class="btn-primary w-full py-2.5"
        :disabled="savingPassword"
        @click="savePassword"
      >
        {{ savingPassword ? '提交中…' : '修改密码' }}
      </button>
    </div>
  </div>
</template>
