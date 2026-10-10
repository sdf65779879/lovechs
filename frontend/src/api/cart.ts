import { request } from '@/utils/request'

import type { CartLine } from './types'

/** 我的购物车 */
export function listCart() {
  return request<CartLine[]>({ url: '/v1/cart', method: 'get' })
}

/** 加入购物车 */
export function addCart(data: {
  dishId: number
  quantity?: number
  taste?: string
  remark?: string
}) {
  return request<void>({ url: '/v1/cart', method: 'post', data })
}

/** 修改购物车条目 */
export function updateCart(
  id: number,
  data: { quantity: number; taste?: string; remark?: string },
) {
  return request<void>({ url: `/v1/cart/${id}`, method: 'put', data })
}

/** 删除购物车条目 */
export function removeCart(id: number) {
  return request<void>({ url: `/v1/cart/${id}`, method: 'delete' })
}

/** 清空购物车 */
export function clearCart() {
  return request<void>({ url: '/v1/cart', method: 'delete' })
}
