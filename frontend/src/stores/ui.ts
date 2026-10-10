import { defineStore } from 'pinia'

import { ref } from 'vue'

/**
 * 界面临时状态：购物车抽屉、菜品详情弹层。
 */
export const useUiStore = defineStore('ui', () => {
  const cartDrawerOpen = ref(false)
  const detailDishId = ref<number | null>(null)

  function openCartDrawer() {
    cartDrawerOpen.value = true
  }

  function closeCartDrawer() {
    cartDrawerOpen.value = false
  }

  function openDishDetail(id: number) {
    detailDishId.value = id
  }

  function closeDishDetail() {
    detailDishId.value = null
  }

  return {
    cartDrawerOpen,
    detailDishId,
    openCartDrawer,
    closeCartDrawer,
    openDishDetail,
    closeDishDetail,
  }
})
