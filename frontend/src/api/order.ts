import { request } from '@/utils/request'

import type { Calendar, Order, PageData, RebuyResult } from './types'

/** 提交订单（Idempotency-Key 保证幂等） */
export function submitOrder(
  data: { remark?: string; expectTime?: string },
  idempotencyKey: string,
) {
  return request<Order>({
    url: '/v1/orders',
    method: 'post',
    data,
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

/** 我的订单分页（tab: active / finished） */
export function pageMyOrders(params: {
  tab: string
  pageNum: number
  pageSize: number
}) {
  return request<PageData<Order>>({ url: '/v1/orders', method: 'get', params })
}

/** 订单详情 */
export function getOrder(id: number) {
  return request<Order>({ url: `/v1/orders/${id}`, method: 'get' })
}

/** 取消订单（仅待接单） */
export function cancelOrder(id: number) {
  return request<void>({ url: `/v1/orders/${id}/cancel`, method: 'put' })
}

/** 再次下单 */
export function rebuyOrder(id: number) {
  return request<RebuyResult>({ url: `/v1/orders/${id}/rebuy`, method: 'post' })
}

/** 吃饭日历 */
export function getCalendar(month: string) {
  return request<Calendar>({ url: '/v1/orders/calendar', method: 'get', params: { month } })
}
