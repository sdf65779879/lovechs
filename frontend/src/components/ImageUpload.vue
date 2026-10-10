<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { ref } from 'vue'

import { uploadImage } from '@/api/file'

defineProps<{ modelValue: string }>()
const emit = defineEmits(['update:modelValue'])

const dragging = ref(false)
const uploading = ref(false)

async function handleFile(file: File | undefined) {
  if (!file) return
  uploading.value = true
  try {
    const path = await uploadImage(file)
    emit('update:modelValue', path)
  } finally {
    uploading.value = false
  }
}

function onDrop(event: DragEvent) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  handleFile(file)
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  handleFile(input.files?.[0])
  input.value = ''
}

function clearImage() {
  emit('update:modelValue', '')
}
</script>

<template>
  <div>
    <!-- 已有图片：展示 + 替换/移除 -->
    <div v-if="modelValue" class="relative inline-block">
      <img
        :src="modelValue"
        alt="预览"
        class="h-36 w-48 rounded-xl bg-cream-200 object-cover"
      />
      <div class="absolute inset-0 flex items-center justify-center gap-2 rounded-xl bg-cocoa-900/0 opacity-0 transition hover:bg-cocoa-900/40 hover:opacity-100">
        <label class="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full bg-white text-cocoa-600">
          <Icon icon="ph:arrows-clockwise" width="16" />
          <input type="file" accept="image/jpeg,image/png,image/webp" class="hidden" @change="onPick" />
        </label>
        <button
          type="button"
          class="flex h-8 w-8 items-center justify-center rounded-full bg-white text-berry-500"
          aria-label="移除图片"
          @click="clearImage"
        >
          <Icon icon="ph:trash" width="16" />
        </button>
      </div>
      <div v-if="uploading" class="absolute inset-0 flex items-center justify-center rounded-xl bg-white/70">
        <Icon icon="svg-spinners:ring-resize" class="text-primary-400" width="28" />
      </div>
    </div>

    <!-- 空态：拖拽 / 点击上传 -->
    <label
      v-else
      class="flex h-36 w-48 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed transition"
      :class="
        dragging
          ? 'border-primary-400 bg-primary-50 text-primary-500'
          : 'border-cream-300 bg-cream-100 text-cocoa-400'
      "
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <Icon v-if="uploading" icon="svg-spinners:ring-resize" width="28" />
      <template v-else>
        <Icon icon="ph:cloud-arrow-up" width="28" />
        <span class="text-xs">拖拽或点击上传</span>
        <span class="text-[10px] text-cocoa-300">jpg/png/webp，≤5MB</span>
      </template>
      <input type="file" accept="image/jpeg,image/png,image/webp" class="hidden" @change="onPick" />
    </label>
  </div>
</template>
