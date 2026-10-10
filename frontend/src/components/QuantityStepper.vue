<script setup lang="ts">
import { Icon } from '@iconify/vue'

const props = withDefaults(
  defineProps<{
    modelValue: number
    min?: number
    max?: number
    small?: boolean
  }>(),
  { min: 1, max: 99, small: false },
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

function decrease() {
  if (props.modelValue > props.min) {
    emit('update:modelValue', props.modelValue - 1)
  }
}

function increase() {
  if (props.modelValue < props.max) {
    emit('update:modelValue', props.modelValue + 1)
  }
}
</script>

<template>
  <div class="flex items-center gap-2">
    <button
      type="button"
      :class="[
        'flex items-center justify-center rounded-full border border-primary-200 bg-white text-primary-400 transition active:scale-90 disabled:opacity-40',
        small ? 'h-6 w-6' : 'h-8 w-8',
      ]"
      :disabled="modelValue <= min"
      @click="decrease"
    >
      <Icon icon="ph:minus" :width="small ? 14 : 16" />
    </button>
    <span
      :class="[
        'min-w-[20px] text-center font-medium text-cocoa-700',
        small ? 'text-sm' : 'text-base',
      ]"
      >{{ modelValue }}</span
    >
    <button
      type="button"
      :class="[
        'flex items-center justify-center rounded-full bg-primary-400 text-white transition active:scale-90 disabled:opacity-40',
        small ? 'h-6 w-6' : 'h-8 w-8',
      ]"
      :disabled="modelValue >= max"
      @click="increase"
    >
      <Icon icon="ph:plus" :width="small ? 14 : 16" />
    </button>
  </div>
</template>
