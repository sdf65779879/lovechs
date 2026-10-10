import { request } from '@/utils/request'

import type { Dish } from './types'

/** 我的收藏列表 */
export function listFavorites() {
  return request<Dish[]>({ url: '/v1/favorites', method: 'get' })
}

/** 收藏菜品 */
export function addFavorite(dishId: number) {
  return request<void>({ url: `/v1/favorites/${dishId}`, method: 'post' })
}

/** 取消收藏 */
export function removeFavorite(dishId: number) {
  return request<void>({ url: `/v1/favorites/${dishId}`, method: 'delete' })
}
