import { defineStore } from 'pinia'

import * as cartApi from '@/api/cart'
import type { CartLine } from '@/api/types'
import { computed, ref } from 'vue'

/**
 * 购物车状态：条目列表、总数量、合计爱心值。
 */
export const useCartStore = defineStore('cart', () => {
  const lines = ref<CartLine[]>([])
  const loaded = ref(false)

  /** 购物车总件数 */
  const count = computed(() =>
    lines.value.reduce((sum, line) => sum + line.quantity, 0),
  )

  /** 合计爱心值（仅可购买的菜品） */
  const totalLove = computed(() =>
    lines.value.reduce(
      (sum, line) => (line.available ? sum + line.quantity * line.loveValue : sum),
      0,
    ),
  )

  /** 是否存在不可购买的条目 */
  const hasUnavailable = computed(() => lines.value.some((line) => !line.available))

  async function fetchCart() {
    lines.value = await cartApi.listCart()
    loaded.value = true
  }

  async function addToCart(data: {
    dishId: number
    quantity?: number
    taste?: string
    remark?: string
  }) {
    await cartApi.addCart(data)
    await fetchCart()
  }

  async function changeLine(
    id: number,
    data: { quantity: number; taste?: string; remark?: string },
  ) {
    await cartApi.updateCart(id, data)
    await fetchCart()
  }

  async function removeLine(id: number) {
    await cartApi.removeCart(id)
    await fetchCart()
  }

  async function clear() {
    await cartApi.clearCart()
    lines.value = []
  }

  function reset() {
    lines.value = []
    loaded.value = false
  }

  return {
    lines,
    loaded,
    count,
    totalLove,
    hasUnavailable,
    fetchCart,
    addToCart,
    changeLine,
    removeLine,
    clear,
    reset,
  }
})
