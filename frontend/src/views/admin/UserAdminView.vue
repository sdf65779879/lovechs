<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'

import {
  adminCreateUser,
  adminPageUsers,
  adminResetUserPassword,
  adminToggleUserStatus,
  adminUpdateUser,
} from '@/api/admin'
import type { UserInfo } from '@/api/types'
import { debounce } from '@/utils/datetime'
import { toast } from '@/utils/toast'

const list = ref<UserInfo[]>([])
const total = ref(0)
const loading = ref(true)
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')

// 编辑/新增弹窗
const dialogOpen = ref(false)
const isCreate = ref(true)
const saving = ref(false)
const form = ref({
  id: 0,
  username: '',
  password: '',
  nickname: '',
  role: 'USER',
})

// 重置密码弹窗
const resetOpen = ref(false)
const resetTarget = ref<UserInfo | null>(null)
const resetPasswordValue = ref('')

async function load() {
  loading.value = true
  try {
    const page = await adminPageUsers({
      keyword: keyword.value.trim() || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

const onSearch = debounce(() => {
  pageNum.value = 1
  load()
}, 300)

function totalPages() {
  return Math.max(1, Math.ceil(total.value / pageSize))
}

function goPage(page: number) {
  pageNum.value = Math.min(Math.max(1, page), totalPages())
  load()
}

function openCreate() {
  isCreate.value = true
  form.value = { id: 0, username: '', password: '', nickname: '', role: 'USER' }
  dialogOpen.value = true
}

function openEdit(user: UserInfo) {
  isCreate.value = false
  form.value = {
    id: user.id,
    username: user.username,
    password: '',
    nickname: user.nickname,
    role: user.role,
  }
  dialogOpen.value = true
}

async function save() {
  if (isCreate.value) {
    if (!form.value.username.trim() || !form.value.password || !form.value.nickname.trim()) {
      toast('请填写账号、密码和昵称')
      return
    }
    if (form.value.password.length < 6) {
      toast('密码至少 6 位')
      return
    }
    saving.value = true
    try {
      await adminCreateUser({
        username: form.value.username.trim(),
        password: form.value.password,
        nickname: form.value.nickname.trim(),
        role: form.value.role,
      })
      toast('用户已新增')
    } finally {
      saving.value = false
    }
  } else {
    if (!form.value.nickname.trim()) {
      toast('请填写昵称')
      return
    }
    saving.value = true
    try {
      await adminUpdateUser(form.value.id, {
        nickname: form.value.nickname.trim(),
        role: form.value.role,
      })
      toast('用户信息已更新')
    } finally {
      saving.value = false
    }
  }
  dialogOpen.value = false
  await load()
}

async function toggleStatus(user: UserInfo) {
  await adminToggleUserStatus(user.id, user.status === 1 ? 0 : 1)
  await load()
}

function openReset(user: UserInfo) {
  resetTarget.value = user
  resetPasswordValue.value = ''
  resetOpen.value = true
}

async function confirmReset() {
  if (!resetTarget.value) return
  if (resetPasswordValue.value.length < 6) {
    toast('新密码至少 6 位')
    return
  }
  await adminResetUserPassword(resetTarget.value.id, resetPasswordValue.value)
  toast('密码已重置')
  resetOpen.value = false
}

onMounted(load)
</script>

<template>
  <div class="space-y-4">
    <!-- 工具条 -->
    <div class="card flex flex-wrap items-center gap-3 p-4">
      <div class="relative min-w-[200px] flex-1">
        <Icon
          icon="ph:magnifying-glass"
          class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-cocoa-300"
          width="16"
        />
        <input
          v-model="keyword"
          class="w-full rounded-pill border border-cream-300 bg-white py-2 pl-9 pr-3 text-sm text-cocoa-600 outline-none placeholder:text-cocoa-300 focus:border-primary-300"
          placeholder="搜索账号或昵称"
          @input="onSearch"
        />
      </div>
      <button type="button" class="btn-primary py-2" @click="openCreate">
        <Icon icon="ph:plus" /> 新增用户
      </button>
    </div>

    <!-- 表格 -->
    <div class="card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[760px] text-sm">
          <thead>
            <tr class="bg-cream-100 text-left text-xs text-cocoa-400">
              <th class="px-4 py-3 font-normal">账号</th>
              <th class="px-4 py-3 font-normal">昵称</th>
              <th class="px-4 py-3 font-normal">角色</th>
              <th class="px-4 py-3 font-normal">状态</th>
              <th class="px-4 py-3 text-right font-normal">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-cream-200">
            <tr v-for="user in list" :key="user.id" class="hover:bg-cream-50">
              <td class="px-4 py-3 font-mono text-xs text-cocoa-500">{{ user.username }}</td>
              <td class="px-4 py-3 font-medium text-cocoa-700">{{ user.nickname }}</td>
              <td class="px-4 py-3">
                <span
                  class="rounded-pill px-2.5 py-0.5 text-[11px]"
                  :class="user.role === 'ADMIN' ? 'bg-honey-100 text-honey-600' : 'bg-berry-50 text-berry-500'"
                >
                  {{ user.role === 'ADMIN' ? '大厨' : '宝贝' }}
                </span>
              </td>
              <td class="px-4 py-3">
                <button
                  type="button"
                  class="relative h-6 w-11 rounded-full transition"
                  :class="user.status === 1 ? 'bg-primary-400' : 'bg-cream-300'"
                  @click="toggleStatus(user)"
                >
                  <span
                    class="absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all"
                    :class="user.status === 1 ? 'left-[22px]' : 'left-0.5'"
                  />
                </button>
              </td>
              <td class="px-4 py-3">
                <div class="flex justify-end gap-2">
                  <button
                    type="button"
                    class="flex h-8 items-center gap-1 rounded-lg bg-cream-100 px-2.5 text-xs text-cocoa-500"
                    @click="openReset(user)"
                  >
                    <Icon icon="ph:key" width="14" /> 重置密码
                  </button>
                  <button
                    type="button"
                    class="flex h-8 w-8 items-center justify-center rounded-lg bg-cream-100 text-cocoa-500"
                    aria-label="编辑"
                    @click="openEdit(user)"
                  >
                    <Icon icon="ph:pencil" width="16" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-cream-200 p-3 text-sm text-cocoa-500">
        <span>共 {{ total }} 条</span>
        <div class="flex items-center gap-2">
          <button type="button" class="rounded-lg px-3 py-1.5 disabled:opacity-40" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">
            <Icon icon="ph:caret-left" />
          </button>
          <span>{{ pageNum }} / {{ totalPages() }}</span>
          <button type="button" class="rounded-lg px-3 py-1.5 disabled:opacity-40" :disabled="pageNum >= totalPages()" @click="goPage(pageNum + 1)">
            <Icon icon="ph:caret-right" />
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <transition name="user-dialog">
        <div
          v-if="dialogOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-cocoa-900/40 p-4"
          @click.self="dialogOpen = false"
        >
          <div class="w-full max-w-sm rounded-2xl bg-white p-5 shadow-warm">
            <p class="mb-4 text-base font-bold text-cocoa-700">
              {{ isCreate ? '新增用户' : '编辑用户' }}
            </p>

            <label class="mb-3 block">
              <span class="mb-1 block text-sm text-cocoa-500">账号</span>
              <input
                v-model="form.username"
                class="input"
                :disabled="!isCreate"
                placeholder="登录账号"
              />
            </label>

            <label v-if="isCreate" class="mb-3 block">
              <span class="mb-1 block text-sm text-cocoa-500">初始密码</span>
              <input v-model="form.password" type="password" class="input" placeholder="至少 6 位" />
            </label>

            <label class="mb-3 block">
              <span class="mb-1 block text-sm text-cocoa-500">昵称</span>
              <input v-model="form.nickname" class="input" placeholder="怎么称呼 TA" />
            </label>

            <label class="mb-5 block">
              <span class="mb-1 block text-sm text-cocoa-500">角色</span>
              <select v-model="form.role" class="input">
                <option value="USER">宝贝（点餐用户）</option>
                <option value="ADMIN">大厨（管理员）</option>
              </select>
            </label>

            <div class="flex gap-3">
              <button type="button" class="btn-ghost flex-1 py-2.5" @click="dialogOpen = false">取消</button>
              <button type="button" class="btn-primary flex-1 py-2.5" :disabled="saving" @click="save">
                {{ saving ? '保存中…' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>

    <!-- 重置密码弹窗 -->
    <Teleport to="body">
      <transition name="user-dialog">
        <div
          v-if="resetOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-cocoa-900/40 p-4"
          @click.self="resetOpen = false"
        >
          <div class="w-full max-w-sm rounded-2xl bg-white p-5 shadow-warm">
            <p class="mb-1 text-base font-bold text-cocoa-700">重置密码</p>
            <p class="mb-4 text-xs text-cocoa-400">
              为「{{ resetTarget?.nickname }}」设置新密码
            </p>
            <input
              v-model="resetPasswordValue"
              type="password"
              class="input mb-4"
              placeholder="新密码（至少 6 位）"
            />
            <div class="flex gap-3">
              <button type="button" class="btn-ghost flex-1 py-2.5" @click="resetOpen = false">取消</button>
              <button type="button" class="btn-primary flex-1 py-2.5" @click="confirmReset">确认重置</button>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
.user-dialog-enter-active,
.user-dialog-leave-active {
  transition: opacity 0.18s ease;
}
.user-dialog-enter-from,
.user-dialog-leave-to {
  opacity: 0;
}
</style>
