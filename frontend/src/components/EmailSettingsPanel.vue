<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'

import {
  adminGetEmailDeliveries,
  adminGetEmailSettings,
  adminSaveEmailSettings,
  adminTestEmail,
  type EmailDelivery,
  type EmailSettings,
} from '@/api/admin'
import { toast } from '@/utils/toast'

const settings = reactive<EmailSettings>({
  provider: 'SMTP',
  smtp: {
    host: '',
    port: 465,
    encryption: 'TLS',
    fromAddress: '',
    username: '',
    passwordConfigured: false,
  },
  channel: { enabled: false, recipients: '', event: 'NEW_ORDER' },
})
const loading = ref(true)
const loadFailed = ref(false)
const saving = ref(false)
const testing = ref(false)
const refreshing = ref(false)
const password = ref('')
const clearPassword = ref(false)
const showPassword = ref(false)
const error = ref('')
const deliveries = ref<EmailDelivery[]>([])
const savedState = ref('')
const state = () => JSON.stringify({ smtp: settings.smtp, channel: settings.channel })
const dirty = computed(
  () => state() !== savedState.value || !!password.value || clearPassword.value,
)
const busy = computed(() => loading.value || saving.value || testing.value)
const statusLabels: Record<EmailDelivery['status'], string> = {
  PENDING: '等待发送',
  SENDING: '发送中',
  RETRY: '等待重试',
  SENT: '已提交邮件服务',
  FAILED: '发送失败',
  SKIPPED: '已跳过',
  MOCK: '模拟发送成功',
}
let poll: ReturnType<typeof setInterval> | undefined
let unmounted = false

function message(e: unknown) {
  const text = e instanceof Error ? e.message : '操作失败，请稍后重试'
  return text
    .replace(/channel\.recipients/g, '大厨收件邮箱')
    .replace(/smtp\.host/g, 'SMTP 主机')
    .replace(/smtp\.fromAddress/g, '发件邮箱')
    .replace(/smtp\.username/g, 'SMTP 用户名')
}

async function refresh() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    deliveries.value = await adminGetEmailDeliveries()
  } catch (e) {
    error.value = `投递记录加载失败：${message(e)}`
  } finally {
    refreshing.value = false
  }
}

async function load() {
  loading.value = true
  loadFailed.value = false
  error.value = ''
  try {
    Object.assign(settings, await adminGetEmailSettings())
    savedState.value = state()
    await refresh()
  } catch (e) {
    loadFailed.value = true
    error.value = message(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const { host, port, encryption, fromAddress, username } = settings.smtp
    await adminSaveEmailSettings({
      smtp: {
        host,
        port,
        encryption,
        fromAddress,
        username,
        password: password.value || undefined,
        clearPassword: clearPassword.value,
      },
      channel: { ...settings.channel },
    })
    password.value = ''
    clearPassword.value = false
    showPassword.value = false
    Object.assign(settings, await adminGetEmailSettings())
    savedState.value = state()
    toast('邮件设置已保存，新订单将使用最新配置')
  } catch (e) {
    error.value = message(e)
  } finally {
    saving.value = false
  }
}

async function test() {
  if (dirty.value) {
    error.value = '请先保存邮件设置，测试邮件使用已保存的系统邮箱和大厨收件地址。'
    return
  }
  testing.value = true
  error.value = ''
  try {
    await adminTestEmail()
    toast(
      settings.provider === 'MOCK'
        ? '模拟测试已加入队列，请查看投递记录'
        : '测试邮件已加入队列，请查看投递记录和收件箱',
    )
    await refresh()
  } catch (e) {
    error.value = message(e)
  } finally {
    testing.value = false
  }
}

onMounted(async () => {
  await load()
  if (unmounted) return
  poll = setInterval(() => {
    if (deliveries.value.some((item) => ['PENDING', 'SENDING', 'RETRY'].includes(item.status)))
      void refresh()
  }, 5000)
})
onUnmounted(() => {
  unmounted = true
  if (poll) clearInterval(poll)
})
</script>

<template>
  <form
    class="card space-y-5 p-5"
    :aria-busy="busy"
    @submit.prevent="save"
  >
    <div>
      <h2 class="text-sm font-bold text-cocoa-700">
        新订单邮件提醒
      </h2>
      <p class="mt-1 text-sm text-cocoa-500">
        顾客下单成功后，自动给大厨发送菜品、口味、备注和用餐时间。
      </p>
    </div>

    <p
      v-if="loading"
      role="status"
      class="text-sm text-cocoa-500"
    >
      正在加载邮件设置…
    </p>
    <p
      v-if="error"
      role="alert"
      class="rounded-xl bg-berry-50 p-3 text-sm text-berry-700"
    >
      {{ error }}
    </p>
    <button
      v-if="loadFailed"
      type="button"
      class="btn-ghost min-h-11"
      @click="load"
    >
      重新加载邮件设置
    </button>

    <template v-if="!loading && !loadFailed">
      <p
        v-if="settings.provider === 'MOCK'"
        role="status"
        class="rounded-xl bg-cream-200 p-3 text-sm text-cocoa-700"
      >
        当前为模拟模式：可验证配置和下单提醒，邮件不会投递到真实邮箱。
      </p>

      <fieldset
        v-if="settings.provider === 'SMTP'"
        :disabled="busy"
        class="space-y-3"
      >
        <legend class="mb-3 text-sm font-bold text-cocoa-700">
          系统发件邮箱
        </legend>
        <label class="block">
          <span class="mb-1 block text-sm text-cocoa-700">SMTP 主机</span>
          <input
            v-model="settings.smtp.host"
            class="input min-h-11"
            maxlength="253"
            placeholder="如 smtp.qq.com"
            autocomplete="off"
          >
        </label>
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="block">
            <span class="mb-1 block text-sm text-cocoa-700">加密方式</span>
            <select
              v-model="settings.smtp.encryption"
              class="input min-h-11"
            >
              <option value="TLS">TLS（通常使用 465）</option>
              <option value="STARTTLS">STARTTLS（通常使用 587）</option>
              <option value="NONE">不加密（内部邮件服务）</option>
            </select>
          </label>
          <label class="block">
            <span class="mb-1 block text-sm text-cocoa-700">SMTP 端口</span>
            <input
              v-model.number="settings.smtp.port"
              type="number"
              required
              min="1"
              max="65535"
              class="input min-h-11"
            >
          </label>
        </div>
        <label class="block">
          <span class="mb-1 block text-sm text-cocoa-700">发件邮箱</span>
          <input
            v-model="settings.smtp.fromAddress"
            type="email"
            class="input min-h-11"
            maxlength="254"
            placeholder="系统使用的发件邮箱"
          >
        </label>
        <label class="block">
          <span class="mb-1 block text-sm text-cocoa-700">SMTP 用户名</span>
          <input
            v-model="settings.smtp.username"
            class="input min-h-11"
            maxlength="254"
            placeholder="通常与发件邮箱相同"
            autocomplete="off"
          >
        </label>
        <label class="block">
          <span class="mb-1 block text-sm text-cocoa-700">邮箱授权码 / SMTP 密码</span>
          <div class="flex gap-2">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="input min-h-11 min-w-0"
              maxlength="512"
              autocomplete="new-password"
              :disabled="clearPassword"
              :placeholder="
                settings.smtp.passwordConfigured
                  ? '已保存，留空保留原授权码'
                  : '填写邮箱服务提供的授权码'
              "
            >
            <button
              type="button"
              class="btn-ghost min-h-11 shrink-0"
              :aria-pressed="showPassword"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <span class="mt-1 block text-xs text-cocoa-500">授权码加密保存，不回显。QQ / 163 等邮箱通常需要在邮箱设置中开启 SMTP
            并生成授权码。</span>
        </label>
        <label
          v-if="settings.smtp.passwordConfigured"
          class="flex min-h-11 items-center gap-2 text-sm text-cocoa-700"
        >
          <input
            v-model="clearPassword"
            type="checkbox"
            @change="password = ''"
          >清除已保存的授权码
        </label>
      </fieldset>

      <fieldset
        :disabled="busy"
        class="space-y-3 border-t border-cream-200 pt-4"
      >
        <legend class="text-sm font-bold text-cocoa-700">
          大厨通知
        </legend>
        <label
          class="flex min-h-11 items-center gap-3 rounded-xl bg-primary-50 p-3 text-sm text-cocoa-700"
        >
          <input
            v-model="settings.channel.enabled"
            type="checkbox"
            class="h-5 w-5 accent-primary-500"
          >
          开启新订单邮件通知
        </label>
        <label class="block">
          <span class="mb-1 block text-sm text-cocoa-700">大厨收件邮箱</span>
          <input
            v-model="settings.channel.recipients"
            type="text"
            inputmode="email"
            class="input min-h-11"
            maxlength="1500"
            aria-describedby="email-recipients-help"
            placeholder="chef@example.com"
          >
          <span
            id="email-recipients-help"
            class="mt-1 block text-xs text-cocoa-500"
          >最多 5 个邮箱，用逗号或分号分隔；重复地址会自动合并。</span>
        </label>
        <p class="text-sm text-cocoa-500">
          通知事件：顾客新下单。邮件发送不影响接单流程，临时故障最多尝试 3 次。
        </p>
      </fieldset>

      <div class="flex flex-col gap-3 sm:flex-row">
        <button
          type="submit"
          class="btn-primary min-h-11 flex-1"
          :disabled="busy"
        >
          {{ saving ? '保存中…' : '保存邮件设置' }}
        </button>
        <button
          type="button"
          class="btn-ghost min-h-11 flex-1 disabled:opacity-50"
          :disabled="busy || dirty"
          @click="test"
        >
          {{ testing ? '加入队列中…' : '发送测试邮件' }}
        </button>
      </div>
      <p class="text-xs text-cocoa-500">
        {{
          dirty
            ? '设置有未保存的修改，请先保存再测试。'
            : '点击测试会向已保存的大厨邮箱发送测试邮件；无需开启新订单通知。'
        }}
      </p>
    </template>

    <div class="border-t border-cream-200 pt-4">
      <div class="mb-2 flex items-center justify-between gap-3">
        <h3 class="text-sm font-bold text-cocoa-700">
          最近投递记录
        </h3>
        <button
          type="button"
          class="btn-ghost min-h-11 text-sm disabled:opacity-50"
          :disabled="refreshing || loading"
          @click="refresh"
        >
          {{ refreshing ? '刷新中…' : '刷新记录' }}
        </button>
      </div>
      <p
        v-if="!deliveries.length"
        class="py-3 text-sm text-cocoa-500"
      >
        暂无投递记录，保存邮箱后可发送一封测试邮件。
      </p>
      <ul
        v-else
        class="divide-y divide-cream-200"
        aria-live="polite"
      >
        <li
          v-for="item in deliveries"
          :key="item.id"
          class="space-y-1 py-3 text-sm"
        >
          <div class="flex flex-wrap justify-between gap-2">
            <span class="font-medium text-cocoa-700">{{
              item.eventType === 'TEST' ? '测试邮件' : `新订单 #${item.orderId}`
            }}</span>
            <span
              class="tag"
              :class="item.status === 'FAILED' ? 'bg-berry-50 text-berry-700' : 'text-cocoa-700'"
            >{{ statusLabels[item.status] }}</span>
          </div>
          <p class="text-xs text-cocoa-500">
            {{ item.createdAt.replace('T', ' ') }} · 尝试 {{ item.attempts }} / 3 次 ·
            {{ item.recipientCount }} 个收件人
          </p>
          <p
            v-if="item.errorMessage"
            class="break-words text-sm text-berry-700"
          >
            {{ item.errorMessage }}
          </p>
        </li>
      </ul>
      <p class="mt-2 text-xs text-cocoa-500">
        显示最近 20 条记录。发送成功表示邮箱服务已接收，请同时检查收件箱和垃圾邮件。
      </p>
    </div>
  </form>
</template>
