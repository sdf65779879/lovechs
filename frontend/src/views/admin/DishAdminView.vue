<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed, onMounted, ref } from 'vue'

import {
  adminBatchDishStatus,
  adminCreateDish,
  adminListAllCategories,
  adminPageDishes,
  adminRemoveDish,
  adminToggleDishRecommend,
  adminToggleDishStatus,
  adminUpdateDish,
} from '@/api/admin'
import type { Category, Dish } from '@/api/types'
import DishSkeleton from '@/components/DishSkeleton.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import { debounce } from '@/utils/datetime'
import { toast } from '@/utils/toast'

const categories = ref<Category[]>([])
const list = ref<Dish[]>([])
const total = ref(0)
const loading = ref(true)
const pageNum = ref(1)
const pageSize = 10

// 筛选
const filterCategory = ref<number | undefined>(undefined)
const filterStatus = ref<number | undefined>(undefined)
const filterKeyword = ref('')

// 选择
const selectedIds = ref<number[]>([])

// 弹窗
const dialogOpen = ref(false)
const editing = ref<Dish | null>(null)
const saving = ref(false)
const form = ref({
  image: '',
  name: '',
  categoryId: undefined as number | undefined,
  description: '',
  tagsInput: '',
  loveValue: 5,
  sort: 0,
  status: 1,
  isRecommend: 0,
})

function categoryName(id: number) {
  return categories.value.find((category) => category.id === id)?.name ?? '—'
}

async function load() {
  loading.value = true
  try {
    const page = await adminPageDishes({
      categoryId: filterCategory.value,
      status: filterStatus.value,
      keyword: filterKeyword.value.trim() || undefined,
      pageNum: pageNum.value,
      pageSize,
    })
    list.value = page.list
    total.value = page.total
    selectedIds.value = []
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

function toggleSelect(id: number) {
  if (selectedIds.value.includes(id)) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  } else {
    selectedIds.value = [...selectedIds.value, id]
  }
}

function toggleSelectAll() {
  if (selectedIds.value.length === list.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = list.value.map((dish) => dish.id)
  }
}

async function batchStatus(status: number) {
  if (selectedIds.value.length === 0) return
  await adminBatchDishStatus(selectedIds.value, status)
  toast(status === 1 ? '已批量上架' : '已批量下架')
  await load()
}

async function toggleStatus(dish: Dish) {
  await adminToggleDishStatus(dish.id, dish.status === 1 ? 0 : 1)
  await load()
}

async function toggleRecommend(dish: Dish) {
  await adminToggleDishRecommend(dish.id, dish.isRecommend === 1 ? 0 : 1)
  await load()
}

async function remove(dish: Dish) {
  await adminRemoveDish(dish.id)
  toast('菜品已删除')
  await load()
}

function openCreate() {
  editing.value = null
  form.value = {
    image: '',
    name: '',
    categoryId: categories.value[0]?.id,
    description: '',
    tagsInput: '',
    loveValue: 5,
    sort: total.value + 1,
    status: 1,
    isRecommend: 0,
  }
  dialogOpen.value = true
}

function openEdit(dish: Dish) {
  editing.value = dish
  form.value = {
    image: dish.image,
    name: dish.name,
    categoryId: dish.categoryId,
    description: dish.description,
    tagsInput: dish.tagList.join('、'),
    loveValue: dish.loveValue,
    sort: dish.sort,
    status: dish.status,
    isRecommend: dish.isRecommend,
  }
  dialogOpen.value = true
}

/** 标签输入统一支持中英文逗号分隔 */
function parseTags(text: string): string[] {
  return text
    .split(/[,，、]/)
    .map((tag) => tag.trim())
    .filter(Boolean)
}

async function save() {
  if (!form.value.name.trim()) {
    toast('请填写菜品名称')
    return
  }
  if (!form.value.categoryId) {
    toast('请选择分类')
    return
  }
  saving.value = true
  try {
    const payload = {
      image: form.value.image,
      name: form.value.name.trim(),
      categoryId: form.value.categoryId,
      description: form.value.description.trim(),
      tags: parseTags(form.value.tagsInput).join(','),
      loveValue: Number(form.value.loveValue) || 0,
      sort: Number(form.value.sort) || 0,
      status: form.value.status,
      isRecommend: form.value.isRecommend,
    }
    if (editing.value) {
      await adminUpdateDish(editing.value.id, payload)
      toast('菜品已更新')
    } else {
      await adminCreateDish(payload)
      toast('菜品已新增')
    }
    dialogOpen.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  categories.value = await adminListAllCategories()
  await load()
})
</script>

<template>
  <div class="space-y-4">
    <!-- 筛选工具条 -->
    <div class="card flex flex-wrap items-center gap-3 p-4">
      <select
        v-model="filterCategory"
        class="rounded-pill border border-cream-300 bg-white px-3 py-2 text-sm text-cocoa-600 outline-none"
        @change="pageNum = 1; load()"
      >
        <option :value="undefined">全部分类</option>
        <option v-for="category in categories" :key="category.id" :value="category.id">
          {{ category.name }}
        </option>
      </select>

      <select
        v-model="filterStatus"
        class="rounded-pill border border-cream-300 bg-white px-3 py-2 text-sm text-cocoa-600 outline-none"
        @change="pageNum = 1; load()"
      >
        <option :value="undefined">全部状态</option>
        <option :value="1">已上架</option>
        <option :value="0">已下架</option>
      </select>

      <div class="relative min-w-[180px] flex-1">
        <Icon
          icon="ph:magnifying-glass"
          class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-cocoa-300"
          width="16"
        />
        <input
          v-model="filterKeyword"
          class="w-full rounded-pill border border-cream-300 bg-white py-2 pl-9 pr-3 text-sm text-cocoa-600 outline-none placeholder:text-cocoa-300 focus:border-primary-300"
          placeholder="搜索菜品名称"
          @input="onSearch"
        />
      </div>

      <button type="button" class="btn-primary py-2" @click="openCreate">
        <Icon icon="ph:plus" /> 新增菜品
      </button>
    </div>

    <!-- 批量操作条 -->
    <div v-if="selectedIds.length" class="card flex items-center gap-3 p-3">
      <span class="text-sm text-cocoa-500">已选 {{ selectedIds.length }} 项</span>
      <button type="button" class="btn-ghost py-1.5 text-xs" @click="batchStatus(1)">批量上架</button>
      <button type="button" class="btn-ghost py-1.5 text-xs" @click="batchStatus(0)">批量下架</button>
    </div>

    <!-- 表格 -->
    <div class="card overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full min-w-[820px] text-sm">
          <thead>
            <tr class="bg-cream-100 text-left text-xs text-cocoa-400">
              <th class="px-4 py-3">
                <input
                  type="checkbox"
                  class="h-4 w-4 accent-primary-400"
                  :checked="selectedIds.length === list.length && list.length > 0"
                  @change="toggleSelectAll"
                />
              </th>
              <th class="px-4 py-3 font-normal">图片</th>
              <th class="px-4 py-3 font-normal">名称</th>
              <th class="px-4 py-3 font-normal">分类</th>
              <th class="px-4 py-3 font-normal">爱心值</th>
              <th class="px-4 py-3 font-normal">排序</th>
              <th class="px-4 py-3 font-normal">推荐</th>
              <th class="px-4 py-3 font-normal">上架</th>
              <th class="px-4 py-3 text-right font-normal">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-cream-200">
            <tr v-for="dish in list" :key="dish.id" class="hover:bg-cream-50">
              <td class="px-4 py-3">
                <input
                  type="checkbox"
                  class="h-4 w-4 accent-primary-400"
                  :checked="selectedIds.includes(dish.id)"
                  @change="toggleSelect(dish.id)"
                />
              </td>
              <td class="px-4 py-3">
                <img
                  :src="dish.image"
                  :alt="dish.name"
                  class="h-11 w-14 rounded-lg bg-cream-200 object-cover"
                />
              </td>
              <td class="px-4 py-3">
                <p class="font-medium text-cocoa-700">{{ dish.name }}</p>
                <p class="mt-0.5 max-w-[180px] truncate text-xs text-cocoa-400">
                  {{ dish.description }}
                </p>
              </td>
              <td class="px-4 py-3 text-cocoa-500">{{ categoryName(dish.categoryId) }}</td>
              <td class="px-4 py-3">
                <span class="flex items-center gap-0.5 text-primary-500">
                  <Icon icon="ph:heart-fill" width="13" />{{ dish.loveValue }}
                </span>
              </td>
              <td class="px-4 py-3 text-cocoa-400">{{ dish.sort }}</td>
              <td class="px-4 py-3">
                <button
                  type="button"
                  class="text-lg transition"
                  :class="dish.isRecommend ? 'text-honey-500' : 'text-cream-300'"
                  :aria-label="dish.isRecommend ? '取消推荐' : '设为推荐'"
                  @click="toggleRecommend(dish)"
                >
                  <Icon :icon="dish.isRecommend ? 'ph:star-fill' : 'ph:star'" />
                </button>
              </td>
              <td class="px-4 py-3">
                <button
                  type="button"
                  class="relative h-6 w-11 rounded-full transition"
                  :class="dish.status === 1 ? 'bg-primary-400' : 'bg-cream-300'"
                  @click="toggleStatus(dish)"
                >
                  <span
                    class="absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all"
                    :class="dish.status === 1 ? 'left-[22px]' : 'left-0.5'"
                  />
                </button>
              </td>
              <td class="px-4 py-3">
                <div class="flex justify-end gap-2">
                  <button
                    type="button"
                    class="flex h-8 w-8 items-center justify-center rounded-lg bg-cream-100 text-cocoa-500"
                    aria-label="编辑"
                    @click="openEdit(dish)"
                  >
                    <Icon icon="ph:pencil" width="16" />
                  </button>
                  <button
                    type="button"
                    class="flex h-8 w-8 items-center justify-center rounded-lg bg-berry-50 text-berry-500"
                    aria-label="删除"
                    @click="remove(dish)"
                  >
                    <Icon icon="ph:trash" width="16" />
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
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 disabled:opacity-40"
            :disabled="pageNum <= 1"
            @click="goPage(pageNum - 1)"
          >
            <Icon icon="ph:caret-left" />
          </button>
          <span>{{ pageNum }} / {{ totalPages() }}</span>
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 disabled:opacity-40"
            :disabled="pageNum >= totalPages()"
            @click="goPage(pageNum + 1)"
          >
            <Icon icon="ph:caret-right" />
          </button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <transition name="dish-dialog">
        <div
          v-if="dialogOpen"
          class="fixed inset-0 z-50 bg-cocoa-900/40"
          @click.self="dialogOpen = false"
        />
      </transition>

      <transition name="dish-pop">
        <div
          v-if="dialogOpen"
          class="pointer-events-none fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          <div
            class="pointer-events-auto flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-2xl bg-white shadow-warm"
          >
            <div class="flex items-center justify-between border-b border-cream-200 px-5 py-4">
              <p class="text-base font-bold text-cocoa-700">
                {{ editing ? '编辑菜品' : '新增菜品' }}
              </p>
              <button type="button" aria-label="关闭" class="text-cocoa-300" @click="dialogOpen = false">
                <Icon icon="ph:x-circle" width="24" />
              </button>
            </div>

            <div class="flex-1 overflow-y-auto p-5">
              <div class="flex gap-5">
                <div>
                  <p class="mb-2 text-sm text-cocoa-500">菜品图片</p>
                  <ImageUpload v-model="form.image" />
                </div>
                <div class="flex-1 space-y-3">
                  <label class="block">
                    <span class="mb-1 block text-sm text-cocoa-500">名称</span>
                    <input v-model="form.name" class="input" maxlength="30" placeholder="给菜品起个名字" />
                  </label>
                  <label class="block">
                    <span class="mb-1 block text-sm text-cocoa-500">分类</span>
                    <select v-model="form.categoryId" class="input">
                      <option v-for="category in categories" :key="category.id" :value="category.id">
                        {{ category.name }}
                      </option>
                    </select>
                  </label>
                </div>
              </div>

              <label class="mt-3 block">
                <span class="mb-1 block text-sm text-cocoa-500">描述</span>
                <textarea
                  v-model="form.description"
                  rows="2"
                  maxlength="200"
                  class="input resize-none"
                  placeholder="这道菜的故事或做法"
                />
              </label>

              <label class="mt-3 block">
                <span class="mb-1 block text-sm text-cocoa-500">标签（逗号分隔）</span>
                <input
                  v-model="form.tagsInput"
                  class="input"
                  placeholder="如：微辣、清淡、拿手菜"
                />
              </label>

              <div class="mt-3 grid grid-cols-2 gap-4">
                <label class="block">
                  <span class="mb-1 block text-sm text-cocoa-500">爱心值</span>
                  <input v-model.number="form.loveValue" type="number" min="0" max="999" class="input" />
                </label>
                <label class="block">
                  <span class="mb-1 block text-sm text-cocoa-500">排序（越小越靠前）</span>
                  <input v-model.number="form.sort" type="number" min="0" class="input" />
                </label>
              </div>

              <div class="mt-4 flex gap-6">
                <label class="flex items-center gap-2 text-sm text-cocoa-500">
                  <input
                    v-model="form.status"
                    type="checkbox"
                    :true-value="1"
                    :false-value="0"
                    class="h-4 w-4 accent-primary-400"
                  />
                  立即上架
                </label>
                <label class="flex items-center gap-2 text-sm text-cocoa-500">
                  <input
                    v-model="form.isRecommend"
                    type="checkbox"
                    :true-value="1"
                    :false-value="0"
                    class="h-4 w-4 accent-honey-400"
                  />
                  设为推荐
                </label>
              </div>
            </div>

            <div class="flex gap-3 border-t border-cream-200 px-5 py-3">
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
.dish-dialog-enter-active,
.dish-dialog-leave-active {
  transition: opacity 0.18s ease;
}
.dish-dialog-enter-from,
.dish-dialog-leave-to {
  opacity: 0;
}

.dish-pop-enter-active,
.dish-pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dish-pop-enter-from,
.dish-pop-leave-to {
  opacity: 0;
  transform: scale(0.94);
}</style>
