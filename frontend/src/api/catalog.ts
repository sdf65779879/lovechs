import { request } from '@/utils/request'

import type { Category, Dish, PageData } from './types'

/** 启用中的分类列表 */
export function listCategories() {
  return request<Category[]>({ url: '/v1/categories', method: 'get' })
}

/** 菜品分页（仅上架） */
export function pageDishes(params: {
  categoryId?: number
  keyword?: string
  pageNum: number
  pageSize: number
}) {
  return request<PageData<Dish>>({ url: '/v1/dishes', method: 'get', params })
}

/** 推荐菜品 */
export function listRecommend(limit = 8) {
  return request<Dish[]>({ url: '/v1/dishes/recommend', method: 'get', params: { limit } })
}

/** 菜品详情 */
export function getDish(id: number) {
  return request<Dish>({ url: `/v1/dishes/${id}`, method: 'get' })
}
