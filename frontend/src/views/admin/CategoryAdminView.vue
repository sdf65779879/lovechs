<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { onMounted, ref } from 'vue'

import {
  adminCreateCategory,
  adminPageCategories,
  adminRemoveCategory,
  adminToggleCategoryStatus,
  adminUpdateCategory,
} from '@/api/admin'
import type { Category } from '@/api/types'
import { toast } from '@/utils/toast'

const list = ref<Category[]>([])
const loading = ref(true)

// 弹窗状态
const dialogOpen = ref(false)
const editing = ref<Category | null>(null)
const formName = ref('')
const formSort = ref(0)
const formStatus = ref(1)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await adminPageCategories({ pageNum: 1, pageSize: 100 })
    list.value = page.list
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  formName.value = ''
  formSort.value = list.value.length + 1
  formStatus.value = 1
  dialogOpen.value = true
}

function openEdit(category: Category) {
  editing.value = category
  formName.value = category.name
  formSort.value = category.sort
  formStatus.value = category.status
  dialogOpen.value = true
}

async function save() {
  if (!formName.value.trim()) {
    toast('请填写分类名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: formName.value.trim(),
      sort: formSort.value,
      status: formStatus.value,
    }
    if (editing.value) {
      await adminUpdateCategory(editing.value.id, payload)
      toast('分类已更新')
    } else {
      await adminCreateCategory(payload)
      toast('分类已新增')
    }
    dialogOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(category: Category) {
  const next = category.status === 1 ? 0 : 1
  await adminToggleCategoryStatus(category.id, next)
  await load()
}

async function remove(category: Category) {
  await adminRemoveCategory(category.id)
  toast('分类已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="card overflow-hidden">
    <!-- 工具条 -->
    <div class="flex items-center justify-between border-b border-cream-200 p-4">
      <p class="text-sm text-cocoa-400">共 {{ list.length }} 个分类</p>
      <button type="button" class="btn-primary py-2" @click="openCreate">
        <Icon icon="ph:plus" /> 新增分类
      </button>
    </div>

    <!-- 表格 -->
    <div class="overflow-x-auto">
      <table class="w-full min-w-[520px] text-sm">
        <thead>
          <tr class="bg-cream-100 text-left text-xs text-cocoa-400">
            <th class="px-4 py-3 font-normal">排序</th>
            <th class="px-4 py-3 font-normal">分类名称</th>
            <th class="px-4 py-3 font-normal">状态</th>
            <th class="px-4 py-3 text-right font-normal">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-cream-200">
          <tr v-for="category in list" :key="category.id" class="hover:bg-cream-50">
            <td class="px-4 py-3 text-cocoa-400">{{ category.sort }}</td>
            <td class="px-4 py-3 font-medium text-cocoa-700">{{ category.name }}</td>
            <td class="px-4 py-3">
              <button
                type="button"
                class="relative h-6 w-11 rounded-full transition"
                :class="category.status === 1 ? 'bg-primary-400' : 'bg-cream-300'"
                :aria-label="category.status === 1 ? '点击禁用' : '点击启用'"
                @click="toggleStatus(category)"
              >
                <span
                  class="absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all"
                  :class="category.status === 1 ? 'left-[22px]' : 'left-0.5'"
                />
              </button>
            </td>
            <td class="px-4 py-3">
              <div class="flex justify-end gap-2">
                <button
                  type="button"
                  class="flex h-8 w-8 items-center justify-center rounded-lg bg-cream-100 text-cocoa-500"
                  aria-label="编辑"
                  @click="openEdit(category)"
                >
                  <Icon icon="ph:pencil" width="16" />
                </button>
                <button
                  type="button"
                  class="flex h-8 w-8 items-center justify-center rounded-lg bg-berry-50 text-berry-500"
                  aria-label="删除"
                  @click="remove(category)"
                >
                  <Icon icon="ph:trash" width="16" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <transition name="dialog-fade">
        <div
          v-if="dialogOpen"
          class="fixed inset-0 z-50 flex items-center justify-center bg-cocoa-900/40 p-4"
          @click.self="dialogOpen = false"
        >
          <div class="w-full max-w-sm rounded-2xl bg-white p-5 shadow-warm">
            <p class="mb-4 text-base font-bold text-cocoa-700">
              {{ editing ? '编辑分类' : '新增分类' }}
            </p>
            <label class="mb-3 block">
              <span class="mb-1 block text-sm text-cocoa-500">名称</span>
              <input v-model="formName" class="input" placeholder="如：主食、小炒" maxlength="20" />
            </label>
            <label class="mb-3 block">
              <span class="mb-1 block text-sm text-cocoa-500">排序（越小越靠前）</span>
              <input v-model.number="formSort" type="number" min="0" class="input" />
            </label>
            <label class="mb-4 flex items-center gap-2 text-sm text-cocoa-500">
              <input v-model="formStatus" type="checkbox" :true-value="1" :false-value="0" class="h-4 w-4 accent-primary-400" />
              启用（用户端可见）
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
  </div>
</template>

<style scoped>
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.18s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
