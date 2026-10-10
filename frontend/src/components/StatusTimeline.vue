<script setup lang="ts">
import { Icon } from '@iconify/vue'
import { computed } from 'vue'

import type { Order } from '@/api/types'
import { shortTime } from '@/utils/datetime'

const props = defineProps<{ order: Order }>()

const STEPS = [
  { status: 'PENDING', label: '待接单', desc: '大厨马上就来' },
  { status: 'ACCEPTED', label: '已接单', desc: '大厨已接单' },
  { status: 'COOKING', label: '制作中', desc: '正在为你烹饪' },
  { status: 'COMPLETED', label: '已完成', desc: '趁热享用吧' },
]

const STATUS_INDEX: Record<string, number> = {
  PENDING: 0,
  ACCEPTED: 1,
  COOKING: 2,
  COMPLETED: 3,
}

const currentIndex = computed(() => STATUS_INDEX[props.order.status] ?? -1)
const isTerminalBad = computed(() =>
  ['CANCELLED', 'REJECTED'].includes(props.order.status),
)

/** 每一步对应的时间（制作中无独立时间字段，复用接单时间） */
function stepTime(index: number): string {
  if (index === 0) return shortTime(props.order.createTime)
  if (index === 1) return shortTime(props.order.acceptTime)
  if (index === 2) return shortTime(props.order.acceptTime)
  if (index === 3) return shortTime(props.order.finishTime)
  return ''
}
</script>

<template>
  <div>
    <!-- 取消 / 拒绝特殊态 -->
    <div
      v-if="isTerminalBad"
      class="flex items-start gap-3 rounded-2xl bg-berry-50 p-4"
    >
      <Icon
        :icon="order.status === 'CANCELLED' ? 'ph:x-circle' : 'ph-warning-circle'"
        class="mt-0.5 flex-none text-berry-400"
        width="24"
      />
      <div>
        <p class="text-sm font-medium text-berry-500">
          {{ order.status === 'CANCELLED' ? '订单已取消' : '订单被拒绝' }}
        </p>
        <p v-if="order.rejectReason" class="mt-1 text-xs text-cocoa-500">
          理由：{{ order.rejectReason }}
        </p>
      </div>
    </div>

    <!-- 正常时间轴 -->
    <div v-else class="flex items-start">
      <template v-for="(step, index) in STEPS" :key="step.status">
        <div class="flex flex-1 flex-col items-center text-center">
          <div
            class="flex h-9 w-9 items-center justify-center rounded-full transition"
            :class="[
              index <= currentIndex
                ? 'bg-primary-400 text-white'
                : 'bg-cream-200 text-cocoa-300',
              index === currentIndex ? 'animate-soft-bounce' : '',
            ]"
          >
            <Icon
              :icon="
                index < currentIndex
                  ? 'ph:check'
                  : [
                      'ph:bell-ringing',
                      'ph:hand-tap',
                      'ph:cooking-pot',
                      'ph:bowl-food',
                    ][index]
              "
              width="18"
            />
          </div>
          <p
            class="mt-1.5 text-xs font-medium"
            :class="index <= currentIndex ? 'text-cocoa-700' : 'text-cocoa-300'"
          >
            {{ step.label }}
          </p>
          <p class="mt-0.5 h-4 text-[10px] text-cocoa-300">{{ stepTime(index) }}</p>
        </div>

        <!-- 连接线 -->
        <div
          v-if="index < STEPS.length - 1"
          class="mt-[18px] h-0.5 flex-1 rounded"
          :class="index < currentIndex ? 'bg-primary-400' : 'bg-cream-200'"
        />
      </template>
    </div>
  </div>
</template>
