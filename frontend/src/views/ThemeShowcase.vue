<script setup lang="ts">
import { Icon } from '@iconify/vue'

const swatches = [
  { name: '奶油橘', cls: 'bg-primary-400' },
  { name: '草莓粉', cls: 'bg-berry-400' },
  { name: '蜂蜜黄', cls: 'bg-honey-400' },
  { name: '奶白', cls: 'bg-cream-200 border border-cream-300' },
  { name: '暖棕', cls: 'bg-cocoa-700' },
]
</script>

<template>
  <div class="min-h-full bg-cream-100 px-5 pb-16 pt-10">
    <div class="mx-auto max-w-md">
      <div class="mb-6 text-center">
        <div class="mb-2 flex items-center justify-center gap-2 text-2xl font-bold text-cocoa-700">
          <Icon icon="ph:bowl-food" class="text-primary-400" width="30" />
          情侣点餐 · 主题预览
        </div>
        <p class="text-sm text-cocoa-500">暖色温馨风 · 移动端优先</p>
      </div>

      <!-- 色板 -->
      <div class="card mb-5 p-4">
        <p class="mb-3 text-sm font-medium text-cocoa-700">主题色板</p>
        <div class="flex justify-between gap-2">
          <div v-for="s in swatches" :key="s.name" class="flex flex-1 flex-col items-center gap-1">
            <div :class="['h-10 w-10 rounded-full shadow-card', s.cls]" />
            <span class="text-[10px] text-cocoa-500">{{ s.name }}</span>
          </div>
        </div>
      </div>

      <!-- 按钮 -->
      <div class="card mb-5 space-y-3 p-4">
        <p class="text-sm font-medium text-cocoa-700">按钮样式</p>
        <div class="flex flex-wrap gap-3">
          <button class="btn-primary">
            <Icon icon="ph:heart" /> 加入爱心
          </button>
          <button class="btn-berry">
            <Icon icon="ph:shopping-cart" /> 去下单
          </button>
          <button class="btn-ghost">
            <Icon icon="ph:star" /> 次按钮
          </button>
        </div>
      </div>

      <!-- 表单 -->
      <div class="card mb-5 space-y-3 p-4">
        <p class="text-sm font-medium text-cocoa-700">表单元素</p>
        <input class="input" placeholder="想对大厨说点什么？" />
        <div class="flex gap-2">
          <span class="tag">清淡</span>
          <span class="tag tag-spicy">微辣</span>
          <span class="tag tag-berry">甜品</span>
          <span class="tag tag-honey">拿手菜</span>
        </div>
      </div>

      <!-- 占位图 -->
      <div class="card mb-5 p-4">
        <p class="mb-3 text-sm font-medium text-cocoa-700">内置暖色占位图（离线可用）</p>
        <div class="grid grid-cols-3 gap-2">
          <img
            v-for="n in 15"
            :key="n"
            :src="`/images/placeholder/${n}.svg`"
            :alt="`placeholder ${n}`"
            class="aspect-[3/2] w-full rounded-lg"
          />
        </div>
      </div>

      <p class="text-center text-xs text-cocoa-300">M10 地基完成后，M11 将实现用户端全部页面</p>
    </div>
  </div>
</template>
