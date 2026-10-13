import { request } from '@/utils/request'

import type { Category, Dashboard, Dish, Order, PageData, SysConfigLine, UserInfo } from './types'

/* ================= 分类管理 ================= */

export function adminPageCategories(params: { pageNum: number; pageSize: number }) {
  return request<PageData<Category>>({ url: '/v1/admin/categories', method: 'get', params })
}

export function adminListAllCategories() {
  return request<Category[]>({ url: '/v1/admin/categories/all', method: 'get' })
}

export function adminCreateCategory(data: { name: string; sort?: number; status?: number }) {
  return request<number>({ url: '/v1/admin/categories', method: 'post', data })
}

export function adminUpdateCategory(
  id: number,
  data: { name: string; sort?: number; status?: number },
) {
  return request<void>({ url: `/v1/admin/categories/${id}`, method: 'put', data })
}

export function adminRemoveCategory(id: number) {
  return request<void>({ url: `/v1/admin/categories/${id}`, method: 'delete' })
}

export function adminToggleCategoryStatus(id: number, status: number) {
  return request<void>({
    url: `/v1/admin/categories/${id}/status`,
    method: 'put',
    data: { status },
  })
}

/* ================= 菜品管理 ================= */

export function adminPageDishes(params: {
  categoryId?: number
  status?: number
  keyword?: string
  pageNum: number
  pageSize: number
}) {
  return request<PageData<Dish>>({ url: '/v1/admin/dishes', method: 'get', params })
}

export function adminCreateDish(data: Record<string, unknown>) {
  return request<number>({ url: '/v1/admin/dishes', method: 'post', data })
}

export function adminUpdateDish(id: number, data: Record<string, unknown>) {
  return request<void>({ url: `/v1/admin/dishes/${id}`, method: 'put', data })
}

export function adminRemoveDish(id: number) {
  return request<void>({ url: `/v1/admin/dishes/${id}`, method: 'delete' })
}

export function adminToggleDishStatus(id: number, status: number) {
  return request<void>({ url: `/v1/admin/dishes/${id}/status`, method: 'put', data: { status } })
}

export function adminToggleDishRecommend(id: number, isRecommend: number) {
  return request<void>({
    url: `/v1/admin/dishes/${id}/recommend`,
    method: 'put',
    data: { isRecommend },
  })
}

export function adminBatchDishStatus(ids: number[], status: number) {
  return request<void>({
    url: '/v1/admin/dishes/batch/status',
    method: 'put',
    data: { ids, status },
  })
}

/* ================= 订单管理 ================= */

export function adminPageOrders(params: {
  status?: string
  date?: string
  pageNum: number
  pageSize: number
}) {
  return request<PageData<Order>>({ url: '/v1/admin/orders', method: 'get', params })
}

export function adminGetOrder(id: number) {
  return request<Order>({ url: `/v1/admin/orders/${id}`, method: 'get' })
}

export function adminChangeOrderStatus(
  id: number,
  data: { status: string; rejectReason?: string },
) {
  return request<Order>({ url: `/v1/admin/orders/${id}/status`, method: 'put', data })
}

/* ================= 用户管理 ================= */

export function adminPageUsers(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request<PageData<UserInfo>>({ url: '/v1/admin/users', method: 'get', params })
}

export function adminCreateUser(data: {
  username: string
  password: string
  nickname: string
  avatar?: string
  role?: string
}) {
  return request<UserInfo>({ url: '/v1/admin/users', method: 'post', data })
}

export function adminUpdateUser(
  id: number,
  data: { nickname?: string; avatar?: string; role?: string },
) {
  return request<void>({ url: `/v1/admin/users/${id}`, method: 'put', data })
}

export function adminToggleUserStatus(id: number, status: number) {
  return request<void>({ url: `/v1/admin/users/${id}/status`, method: 'put', data: { status } })
}

export function adminResetUserPassword(id: number, password: string) {
  return request<void>({
    url: `/v1/admin/users/${id}/password/reset`,
    method: 'put',
    data: { password },
  })
}

/* ================= 系统配置 ================= */

export function adminListConfigs() {
  return request<SysConfigLine[]>({ url: '/v1/admin/configs', method: 'get' })
}

export function adminUpdateConfigs(configs: Record<string, string>) {
  return request<void>({ url: '/v1/admin/configs', method: 'put', data: { configs } })
}

/* ================= 系统邮件通知 ================= */

export interface EmailSettings {
  provider: 'SMTP' | 'MOCK'
  smtp: {
    host: string
    port: number
    encryption: 'TLS' | 'STARTTLS' | 'NONE'
    fromAddress: string
    username: string
    passwordConfigured: boolean
  }
  channel: { enabled: boolean; recipients: string; event: 'NEW_ORDER' }
}

export interface EmailDelivery {
  id: number
  eventType: 'NEW_ORDER' | 'TEST'
  orderId?: number
  status: 'PENDING' | 'SENDING' | 'RETRY' | 'SENT' | 'FAILED' | 'SKIPPED' | 'MOCK'
  attempts: number
  recipientCount: number
  errorMessage: string
  createdAt: string
  updatedAt: string
}

export function adminGetEmailSettings() {
  return request<EmailSettings>({ url: '/v1/admin/email/settings', method: 'get' })
}

export function adminSaveEmailSettings(data: {
  smtp: Omit<EmailSettings['smtp'], 'passwordConfigured'> & {
    password?: string
    clearPassword: boolean
  }
  channel: EmailSettings['channel']
}) {
  return request<void>({ url: '/v1/admin/email/settings', method: 'put', data })
}

export function adminTestEmail() {
  return request<void>({ url: '/v1/admin/email/test', method: 'post' })
}

export function adminGetEmailDeliveries() {
  return request<EmailDelivery[]>({ url: '/v1/admin/email/deliveries', method: 'get' })
}

/* ================= 数据看板 ================= */

export function getDashboard() {
  return request<Dashboard>({ url: '/v1/admin/dashboard', method: 'get' })
}
